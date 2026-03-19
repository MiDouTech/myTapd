package com.miduo.cloud.ticket.controller.aspect;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.application.operationlog.OperationLogApplicationService;
import com.miduo.cloud.ticket.common.enums.ExecuteResultEnum;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.common.security.SecurityUserDetails;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.po.OperationLogPO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 操作日志AOP切面
 * 自动拦截标注了 @OperationLog 的Controller方法，异步写入操作日志
 * PRD §8 AOP 自动记录日志方案
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int ERROR_STACK_MAX_LENGTH = 500;

    private final OperationLogApplicationService operationLogService;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    public OperationLogAspect(OperationLogApplicationService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(com.miduo.cloud.ticket.controller.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLogPO logPO = buildBaseLog(joinPoint);

        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            logPO.setExecuteResult(ExecuteResultEnum.SUCCESS.getCode());
        } catch (Throwable t) {
            throwable = t;
            logPO.setExecuteResult(ExecuteResultEnum.FAILURE.getCode());
            logPO.setErrorMessage(t.getMessage());
            logPO.setErrorStack(buildErrorStack(t));
        } finally {
            long costMillis = System.currentTimeMillis() - startTime;
            logPO.setCostMillis((int) Math.min(costMillis, Integer.MAX_VALUE));
            saveLogAsync(logPO);
        }

        if (throwable != null) {
            throw throwable;
        }
        return result;
    }

    private OperationLogPO buildBaseLog(ProceedingJoinPoint joinPoint) {
        OperationLogPO logPO = new OperationLogPO();
        logPO.setOperateTime(new Date());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        if (annotation != null) {
            logPO.setModuleName(annotation.moduleName());
            logPO.setOperationItem(annotation.operationItem());
            logPO.setLogLevel(annotation.logLevel().getCode());

            if (annotation.recordParams()) {
                logPO.setRequestParams(serializeParams(joinPoint.getArgs()));
            }
        }

        fillUserInfo(logPO);
        fillRequestInfo(logPO);

        return logPO;
    }

    private void fillUserInfo(OperationLogPO logPO) {
        SecurityUserDetails currentUser = SecurityUtil.getCurrentUser();
        if (currentUser != null) {
            logPO.setAccountId(currentUser.getUserId());
            logPO.setOperatorName(currentUser.getName() != null ? currentUser.getName() : "");
        } else {
            logPO.setAccountId(0L);
            logPO.setOperatorName("system");
        }
    }

    private void fillRequestInfo(OperationLogPO logPO) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            logPO.setOperatorIp("");
            logPO.setUserAgent("");
            logPO.setRequestPath("");
            logPO.setRequestMethod("");
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        logPO.setOperatorIp(getClientIp(request));
        String userAgent = request.getHeader("User-Agent");
        logPO.setUserAgent(userAgent != null ? userAgent : "");
        logPO.setRequestPath(request.getRequestURI());
        logPO.setRequestMethod(request.getMethod());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }

    private String serializeParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            if (args.length == 1) {
                return JSON.toJSONString(args[0]);
            }
            return JSON.toJSONString(args);
        } catch (Exception e) {
            log.warn("操作日志序列化请求参数失败", e);
            return null;
        }
    }

    private String buildErrorStack(Throwable t) {
        if (t == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
        StackTraceElement[] elements = t.getStackTrace();
        for (int i = 0; i < Math.min(elements.length, 10); i++) {
            sb.append("\tat ").append(elements[i].toString()).append("\n");
        }
        String stack = sb.toString();
        return stack.length() > ERROR_STACK_MAX_LENGTH ? stack.substring(0, ERROR_STACK_MAX_LENGTH) : stack;
    }

    private void saveLogAsync(OperationLogPO logPO) {
        asyncExecutor.submit(() -> {
            try {
                operationLogService.saveLog(logPO);
            } catch (Exception e) {
                log.error("异步保存操作日志失败", e);
            }
        });
    }
}

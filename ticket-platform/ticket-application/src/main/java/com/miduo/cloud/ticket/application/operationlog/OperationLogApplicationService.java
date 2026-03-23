package com.miduo.cloud.ticket.application.operationlog;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ExecuteResultEnum;
import com.miduo.cloud.ticket.common.enums.LogLevelEnum;
import com.miduo.cloud.ticket.entity.dto.operationlog.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.mapper.OperationLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.po.OperationLogPO;
import org.springframework.stereotype.Service;

import java.sql.SQLSyntaxErrorException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 操作日志应用服务
 * 接口编号段：API000600–API000605
 */
@Service
public class OperationLogApplicationService extends BaseApplicationService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogApplicationService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 保存操作日志（由AOP切面异步调用）
     */
    public void saveLog(OperationLogPO logPO) {
        executeWithMissingTableFallback("写入操作日志", () -> operationLogMapper.insert(logPO), () -> null);
    }

    /**
     * 记录登录与安全审计日志（由认证服务等调用）
     *
     * @param userId         登录用户ID，失败且未知用户时传 null
     * @param userName       操作人姓名，失败时可为空串
     * @param operatorIp     客户端IP
     * @param userAgent      客户端User-Agent
     * @param requestPath    实际请求路径（如 /api/auth/wecom/login）
     * @param requestMethod  HTTP 方法（如 POST）
     * @param operationItem  操作项描述（如"企微扫码登录"、"刷新访问令牌"）
     * @param success        是否成功
     * @param errorMessage   失败原因（成功时传 null）
     */
    public void saveLoginLog(Long userId, String userName, String operatorIp, String userAgent,
                             String requestPath, String requestMethod,
                             String operationItem, boolean success, String errorMessage) {
        OperationLogPO logPO = new OperationLogPO();
        logPO.setOperateTime(new Date());
        logPO.setAccountId(userId != null ? userId : 0L);
        logPO.setOperatorName(userName != null ? userName : "");
        logPO.setOperatorIp(operatorIp != null ? operatorIp : "");
        logPO.setUserAgent(userAgent != null ? userAgent : "");
        logPO.setLogLevel(LogLevelEnum.SECURITY.getCode());
        logPO.setModuleName("认证管理");
        logPO.setOperationItem(operationItem);
        logPO.setRequestPath(requestPath != null ? requestPath : "");
        logPO.setRequestMethod(requestMethod != null ? requestMethod : "POST");
        logPO.setExecuteResult(success ? ExecuteResultEnum.SUCCESS.getCode() : ExecuteResultEnum.FAILURE.getCode());
        if (!success && errorMessage != null) {
            logPO.setErrorMessage(errorMessage);
        }
        try {
            saveLog(logPO);
        } catch (Exception e) {
            log.warn("保存登录日志失败，不影响登录流程", e);
        }
    }

    /**
     * 多条件分页查询操作日志
     * 接口编号：API000600
     */
    public PageOutput<OperationLogListOutput> page(OperationLogPageInput input) {
        return executeWithMissingTableFallback("分页查询操作日志", () -> {
            Page<OperationLogPO> page = new Page<>(input.getPageNum(), input.getPageSize());
            IPage<OperationLogPO> result = operationLogMapper.selectOperationLogPage(
                    page,
                    input.getAccountId(),
                    trimOrNull(input.getOperatorName()),
                    trimOrNull(input.getOperatorIp()),
                    trimOrNull(input.getLogLevel()),
                    null,
                    trimOrNull(input.getModuleName()),
                    trimOrNull(input.getOperationItem()),
                    trimOrNull(input.getOperationDetail()),
                    trimOrNull(input.getExecuteResult()),
                    trimOrNull(input.getStartTime()),
                    trimOrNull(input.getEndTime()),
                    trimOrNull(input.getSortField()),
                    trimOrNull(input.getSortOrder())
            );
            List<OperationLogListOutput> outputs = result.getRecords().stream()
                    .map(this::convertToListOutput)
                    .collect(Collectors.toList());
            return PageOutput.of(outputs, result.getTotal(), input.getPageNum(), input.getPageSize());
        }, () -> PageOutput.of(Collections.emptyList(), 0L, input.getPageNum(), input.getPageSize()));
    }

    /**
     * 获取操作日志详情
     * 接口编号：API000601
     */
    public OperationLogDetailOutput detail(Long id) {
        return executeWithMissingTableFallback("查询操作日志详情", () -> {
            OperationLogPO po = operationLogMapper.selectById(id);
            if (po == null) {
                return null;
            }
            return convertToDetailOutput(po);
        }, () -> null);
    }

    /**
     * 获取日志统计概览（今日数据）
     * 接口编号：API000602
     */
    public OperationLogStatisticsOutput statistics() {
        return executeWithMissingTableFallback("查询操作日志统计", () -> {
            OperationLogStatisticsOutput output = new OperationLogStatisticsOutput();
            output.setTodayTotalCount(operationLogMapper.countTodayTotal());
            output.setTodayFailureCount(operationLogMapper.countTodayFailure());
            output.setTodayActiveUserCount(operationLogMapper.countTodayActiveUsers());
            output.setTodaySecurityAlertCount(operationLogMapper.countTodaySecurityAlert());
            return output;
        }, this::buildEmptyStatistics);
    }

    /**
     * 获取操作模块枚举列表
     * 接口编号：API000604
     */
    public List<String> listModuleNames() {
        return executeWithMissingTableFallback("查询操作模块列表", operationLogMapper::selectDistinctModuleNames,
                Collections::emptyList);
    }

    private OperationLogListOutput convertToListOutput(OperationLogPO po) {
        OperationLogListOutput output = new OperationLogListOutput();
        output.setId(po.getId());
        output.setOperateTime(po.getOperateTime());
        output.setAccountId(po.getAccountId());
        output.setOperatorName(po.getOperatorName());
        output.setOperatorIp(po.getOperatorIp());
        output.setLogLevel(po.getLogLevel());
        LogLevelEnum logLevel = LogLevelEnum.fromCode(po.getLogLevel());
        output.setLogLevelDesc(logLevel.getDesc());
        output.setModuleName(po.getModuleName());
        output.setRequestPath(po.getRequestPath());
        output.setOperationItem(po.getOperationItem());
        output.setExecuteResult(po.getExecuteResult());
        ExecuteResultEnum executeResult = ExecuteResultEnum.fromCode(po.getExecuteResult());
        output.setExecuteResultDesc(executeResult.getDesc());
        return output;
    }

    private OperationLogDetailOutput convertToDetailOutput(OperationLogPO po) {
        OperationLogDetailOutput output = new OperationLogDetailOutput();
        output.setId(po.getId());
        output.setOperateTime(po.getOperateTime());
        output.setAccountId(po.getAccountId());
        output.setOperatorName(po.getOperatorName());
        output.setOperatorIp(po.getOperatorIp());
        output.setUserAgent(po.getUserAgent());
        output.setLogLevel(po.getLogLevel());
        LogLevelEnum logLevel = LogLevelEnum.fromCode(po.getLogLevel());
        output.setLogLevelDesc(logLevel.getDesc());
        output.setModuleName(po.getModuleName());
        output.setRequestPath(po.getRequestPath());
        output.setRequestMethod(po.getRequestMethod());
        output.setOperationItem(po.getOperationItem());
        output.setRequestParams(po.getRequestParams());
        output.setExecuteResult(po.getExecuteResult());
        ExecuteResultEnum executeResult = ExecuteResultEnum.fromCode(po.getExecuteResult());
        output.setExecuteResultDesc(executeResult.getDesc());
        output.setCostMillis(po.getCostMillis());
        output.setChangeRecords(parseChangeRecords(po.getChangeRecords()));
        output.setErrorCode(po.getErrorCode());
        output.setErrorMessage(po.getErrorMessage());
        output.setErrorStack(po.getErrorStack());
        return output;
    }

    private List<ChangeRecordItem> parseChangeRecords(String changeRecordsJson) {
        if (changeRecordsJson == null || changeRecordsJson.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(changeRecordsJson, new TypeReference<List<ChangeRecordItem>>() {});
        } catch (Exception e) {
            log.warn("解析变更记录JSON失败: {}", changeRecordsJson, e);
            return null;
        }
    }

    private String trimOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private OperationLogStatisticsOutput buildEmptyStatistics() {
        OperationLogStatisticsOutput output = new OperationLogStatisticsOutput();
        output.setTodayTotalCount(0L);
        output.setTodayFailureCount(0L);
        output.setTodayActiveUserCount(0L);
        output.setTodaySecurityAlertCount(0L);
        return output;
    }

    private <T> T executeWithMissingTableFallback(String action, Supplier<T> supplier, Supplier<T> fallbackSupplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            if (isSysOperationLogMissing(ex)) {
                log.warn("操作日志表不存在，{}降级处理。请执行最新Flyway迁移以恢复完整能力。", action);
                return fallbackSupplier.get();
            }
            throw ex;
        }
    }

    private boolean isSysOperationLogMissing(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLSyntaxErrorException) {
                SQLSyntaxErrorException sqlException = (SQLSyntaxErrorException) current;
                if ("42S02".equals(sqlException.getSQLState())) {
                    String message = sqlException.getMessage();
                    return message != null && message.contains("sys_operation_log");
                }
            }
            String message = current.getMessage();
            if (message != null
                    && message.contains("sys_operation_log")
                    && (message.contains("doesn't exist") || message.contains("does not exist"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

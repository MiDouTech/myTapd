package com.miduo.cloud.ticket.controller.annotation;

import com.miduo.cloud.ticket.common.enums.LogLevelEnum;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在Controller方法上，由AOP切面自动采集并写入操作日志
 * PRD §8.2 自定义注解 @OperationLog
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作模块名称 */
    String moduleName();

    /** 操作项名称 */
    String operationItem();

    /** 日志级别（默认业务级） */
    LogLevelEnum logLevel() default LogLevelEnum.BUSINESS;

    /** 是否记录请求参数（敏感接口可关闭） */
    boolean recordParams() default true;

    /** 是否记录变更内容 */
    boolean recordChanges() default false;
}

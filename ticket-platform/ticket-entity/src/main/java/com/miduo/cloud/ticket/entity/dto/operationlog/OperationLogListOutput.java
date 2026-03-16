package com.miduo.cloud.ticket.entity.dto.operationlog;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志列表行输出DTO
 * 接口编号：API000600
 * PRD §6.2 分页查询接口详情
 */
@Data
public class OperationLogListOutput implements Serializable {

    private Long id;

    /** 操作时间 */
    private Date operateTime;

    /** 操作账号ID */
    private Long accountId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人IP */
    private String operatorIp;

    /** 日志级别枚举值 */
    private String logLevel;

    /** 日志级别描述 */
    private String logLevelDesc;

    /** 所属应用编码 */
    private String appCode;

    /** 所属应用名称 */
    private String appName;

    /** 操作模块名称 */
    private String moduleName;

    /** 接口请求路径 */
    private String requestPath;

    /** 操作项名称 */
    private String operationItem;

    /** 执行结果枚举值 */
    private String executeResult;

    /** 执行结果描述 */
    private String executeResultDesc;
}

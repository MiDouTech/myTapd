package com.miduo.cloud.ticket.entity.dto.operationlog;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 操作日志详情输出DTO
 * 接口编号：API000601
 * PRD §6.3 日志详情接口详情
 */
@Data
public class OperationLogDetailOutput implements Serializable {

    private Long id;

    /** 操作时间（精确到毫秒） */
    private Date operateTime;

    /** 操作账号ID */
    private Long accountId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人IP */
    private String operatorIp;

    /** 客户端User-Agent */
    private String userAgent;

    /** 日志级别枚举值 */
    private String logLevel;

    /** 日志级别描述 */
    private String logLevelDesc;

    /** 操作模块名称 */
    private String moduleName;

    /** 接口请求路径 */
    private String requestPath;

    /** HTTP请求方式 */
    private String requestMethod;

    /** 操作项名称 */
    private String operationItem;

    /** 请求参数（JSON格式） */
    private String requestParams;

    /** 执行结果枚举值 */
    private String executeResult;

    /** 执行结果描述 */
    private String executeResultDesc;

    /** 接口耗时（毫秒） */
    private Integer costMillis;

    /** 变更记录列表 */
    private List<ChangeRecordItem> changeRecords;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 异常堆栈摘要（前500字符） */
    private String errorStack;
}

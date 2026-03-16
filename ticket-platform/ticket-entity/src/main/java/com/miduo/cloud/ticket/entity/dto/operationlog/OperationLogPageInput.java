package com.miduo.cloud.ticket.entity.dto.operationlog;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询操作日志请求参数
 * 接口编号：API000600
 * PRD §6.2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogPageInput extends PageInput {

    /** 操作开始时间，格式 yyyy-MM-dd HH:mm:ss */
    private String startTime;

    /** 操作结束时间，格式 yyyy-MM-dd HH:mm:ss */
    private String endTime;

    /** 操作账号ID，精确匹配 */
    private Long accountId;

    /** 操作人姓名，模糊匹配 */
    private String operatorName;

    /** 操作人IP，前缀匹配 */
    private String operatorIp;

    /** 日志级别枚举：SYSTEM/BUSINESS/SECURITY/ERROR */
    private String logLevel;

    /** 所属应用编码枚举 */
    private String appCode;

    /** 操作模块名称，模糊匹配 */
    private String moduleName;

    /** 操作项名称，模糊匹配 */
    private String operationItem;

    /** 操作详情关键词，模糊匹配 */
    private String operationDetail;

    /** 执行结果：SUCCESS/FAILURE */
    private String executeResult;

    /** 排序字段，目前支持：operateTime（默认） */
    private String sortField;

    /** 排序方向：asc/desc（默认desc） */
    private String sortOrder;
}

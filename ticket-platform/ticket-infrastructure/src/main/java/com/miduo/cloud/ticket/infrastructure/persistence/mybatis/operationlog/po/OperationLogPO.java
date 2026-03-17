package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 系统操作日志PO
 * PRD §7.1 操作日志表：sys_operation_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_operation_log")
public class OperationLogPO extends BaseEntity {

    @TableField("account_id")
    private Long accountId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operator_ip")
    private String operatorIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("log_level")
    private String logLevel;

    @TableField("module_name")
    private String moduleName;

    @TableField("request_path")
    private String requestPath;

    @TableField("request_method")
    private String requestMethod;

    @TableField("operation_item")
    private String operationItem;

    @TableField("request_params")
    private String requestParams;

    @TableField("change_records")
    private String changeRecords;

    @TableField("execute_result")
    private String executeResult;

    @TableField("cost_millis")
    private Integer costMillis;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("error_stack")
    private String errorStack;

    @TableField("operate_time")
    private Date operateTime;
}

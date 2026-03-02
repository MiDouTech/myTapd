package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或认证已过期"),
    FORBIDDEN(403, "无操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "数据冲突"),

    PARAM_ERROR(1001, "参数校验失败"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_ALREADY_EXISTS(1003, "数据已存在"),
    DATA_STATUS_ERROR(1004, "数据状态异常"),

    TICKET_NOT_FOUND(2001, "工单不存在"),
    TICKET_STATUS_INVALID(2002, "工单状态不允许此操作"),
    TICKET_ASSIGN_FAILED(2003, "工单分派失败"),

    WORKFLOW_TRANSITION_INVALID(3001, "工作流流转不合法"),
    WORKFLOW_NOT_FOUND(3002, "工作流定义不存在"),
    WORKFLOW_VALIDATION_FAILED(3003, "工作流校验失败"),

    SLA_BREACH(4001, "SLA已超时"),

    WECOM_AUTH_FAILED(5001, "企业微信认证失败"),
    WECOM_API_ERROR(5002, "企业微信API调用失败"),
    WECOM_CALLBACK_VERIFY_FAILED(5003, "企业微信回调验证失败"),
    WECOM_MSG_PARSE_FAILED(5004, "企业微信消息解析失败"),

    BUG_REPORT_NOT_FOUND(6001, "Bug简报不存在"),
    BUG_REPORT_STATUS_INVALID(6002, "Bug简报状态不允许此操作"),

    INTERNAL_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;
}

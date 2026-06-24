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
    SLA_POLICY_NOT_FOUND(4002, "SLA策略不存在"),
    SLA_WARNING(4003, "SLA预警"),

    NOTIFICATION_NOT_FOUND(4101, "通知不存在"),
    NOTIFICATION_PREFERENCE_ERROR(4102, "通知偏好配置错误"),

    WECOM_AUTH_FAILED(5001, "企业微信认证失败"),
    WECOM_API_ERROR(5002, "企业微信API调用失败"),
    WECOM_CALLBACK_VERIFY_FAILED(5003, "企业微信回调验证失败"),
    WECOM_MSG_PARSE_FAILED(5004, "企业微信消息解析失败"),

    SSO_DISABLED(5100, "SSO登录未启用"),
    SSO_VALIDATE_FAILED(5101, "SSO交换Token校验失败"),
    SSO_SESSION_EXPIRED(5102, "SSO会话已过期"),
    SSO_REFRESH_FAILED(5103, "SSO会话续期失败"),
    SSO_API_ERROR(5104, "SSO接口调用异常"),
    SSO_STATE_INVALID(5105, "SSO状态参数校验失败"),
    SSO_ACCOUNT_NOT_SYNCED(5106, "未同步企微账号，请联系管理员同步"),

    BUG_REPORT_NOT_FOUND(6001, "Bug简报不存在"),
    BUG_REPORT_STATUS_INVALID(6002, "Bug简报状态不允许此操作"),

    UPLOAD_FAILED(7001, "文件上传失败"),
    PARAM_INVALID(7002, "参数不合法"),

    OPEN_API_APP_NOT_FOUND(8001, "开放接口应用不存在或已禁用"),
    OPEN_API_SIGNATURE_INVALID(8002, "开放接口签名校验失败"),
    OPEN_API_TIMESTAMP_EXPIRED(8003, "开放接口时间戳已过期"),
    OPEN_API_REPLAY_REQUEST(8004, "开放接口请求已重复"),
    OPEN_API_RATE_LIMITED(8005, "开放接口调用过于频繁"),
    OPEN_API_PERMISSION_DENIED(8006, "开放接口权限不足"),
    PLUGIN_DISABLED(8101, "工单插件功能未启用"),
    PLUGIN_LAUNCH_TOKEN_INVALID(8102, "LaunchToken无效或已过期"),
    PLUGIN_LAUNCH_TOKEN_USED(8103, "LaunchToken已使用"),
    PLUGIN_ORIGIN_DENIED(8104, "来源域名未授权"),
    PLUGIN_APP_DISABLED(8105, "接入应用已禁用"),

    INTERNAL_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;
}

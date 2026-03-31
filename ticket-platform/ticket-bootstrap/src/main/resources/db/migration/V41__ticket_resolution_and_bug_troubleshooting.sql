-- 工单处理结论（终态时由关闭/流转备注写入，公开详情展示）
ALTER TABLE `ticket`
    ADD COLUMN `resolution_summary` varchar(2000) DEFAULT NULL COMMENT '处理结论/关闭说明（公开可见，纯文本）' AFTER `closed_at`;

-- 缺陷客服信息：接口排障扩展字段（可选填，公开接口返回时 URL 需脱敏）
ALTER TABLE `ticket_bug_info`
    ADD COLUMN `troubleshoot_request_url` varchar(1000) DEFAULT NULL COMMENT '请求路径或URL（公开侧脱敏）' AFTER `problem_screenshot`,
    ADD COLUMN `troubleshoot_http_status` varchar(20) DEFAULT NULL COMMENT 'HTTP状态码' AFTER `troubleshoot_request_url`,
    ADD COLUMN `troubleshoot_biz_error_code` varchar(100) DEFAULT NULL COMMENT '业务错误码' AFTER `troubleshoot_http_status`,
    ADD COLUMN `troubleshoot_trace_id` varchar(128) DEFAULT NULL COMMENT 'TraceId/RequestId' AFTER `troubleshoot_biz_error_code`,
    ADD COLUMN `troubleshoot_occurred_at` datetime DEFAULT NULL COMMENT '问题发生时间' AFTER `troubleshoot_trace_id`,
    ADD COLUMN `troubleshoot_client_type` varchar(32) DEFAULT NULL COMMENT '客户端类型（H5/MINI_APP/APP/PC/UNKNOWN）' AFTER `troubleshoot_occurred_at`;

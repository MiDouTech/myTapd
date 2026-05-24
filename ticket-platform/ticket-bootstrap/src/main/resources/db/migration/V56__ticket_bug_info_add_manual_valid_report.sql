-- ============================================================
-- V56__ticket_bug_info_add_manual_valid_report.sql
-- 缺陷工单客服信息新增“手工有效报告”字段（关闭状态人工确认）
-- ============================================================

ALTER TABLE `ticket_bug_info`
    ADD COLUMN `manual_valid_report` varchar(8) DEFAULT NULL COMMENT '手工有效报告（YES:是 NO:否）' AFTER `problem_screenshot`;


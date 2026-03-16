-- ============================================================
-- V21__expand_bug_problem_screenshot_length.sql
-- 缺陷工单客服问题截图字段扩容，支持多张截图URL存储
-- ============================================================

ALTER TABLE `ticket_bug_info`
    MODIFY COLUMN `problem_screenshot` text DEFAULT NULL COMMENT '问题截图（多张URL，逗号分隔）';

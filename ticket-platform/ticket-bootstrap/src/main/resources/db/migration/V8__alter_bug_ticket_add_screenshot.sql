-- ============================================================
-- V8__alter_bug_ticket_add_screenshot.sql
-- 缺陷工单扩展：补充截图字段
-- ============================================================

ALTER TABLE `ticket_bug_info`
    ADD COLUMN `problem_screenshot` varchar(1000) DEFAULT NULL COMMENT '问题截图（URL或逗号分隔）' AFTER `scene_code`;

ALTER TABLE `ticket_bug_test_info`
    ADD COLUMN `reproduce_screenshot` varchar(1000) DEFAULT NULL COMMENT '复现截图（URL或逗号分隔）' AFTER `module_name`;

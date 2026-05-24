-- V57：兜底补齐 ticket_bug_info.manual_valid_report 字段，兼容历史环境
ALTER TABLE `ticket_bug_info`
    ADD COLUMN IF NOT EXISTS `manual_valid_report` varchar(8) DEFAULT NULL COMMENT '手工有效报告（YES:是 NO:否）' AFTER `problem_screenshot`;

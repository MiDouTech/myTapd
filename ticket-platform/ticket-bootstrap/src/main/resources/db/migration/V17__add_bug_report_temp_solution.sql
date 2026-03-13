-- Bug简报新增临时解决方案和临时解决日期字段
-- 对应小工具 easy-bug 中的 临时解决方案(tempsolution) 和 临时解决时间(tempsolvetime)

ALTER TABLE `bug_report`
    ADD COLUMN `temp_resolve_date` DATE NULL COMMENT '临时解决日期' AFTER `resolve_date`,
    ADD COLUMN `temp_solution` TEXT NULL COMMENT '临时解决方案' AFTER `solution`;

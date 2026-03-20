-- V26：为分类表新增备注描述和自然语言匹配关键词字段
ALTER TABLE `ticket_category`
    ADD COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '分类备注描述，说明该分类通常涵盖的问题范围' AFTER `is_active`,
    ADD COLUMN `nl_match_keywords` text DEFAULT NULL COMMENT '自然语言匹配关键词，逗号分隔，用于企微消息自动归类' AFTER `remark`;

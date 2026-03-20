-- V27: 将 ticket_log.remark 从 varchar(500) 扩展为 TEXT
-- 原因：字段级变更历史以 JSON 格式存储，单条记录中文本内容（最长 200 字符/字段）
--       经序列化后可超出 500 字节限制，导致写入被截断或抛出异常。
ALTER TABLE `ticket_log`
    MODIFY COLUMN `remark` text COMMENT '操作备注（JSON 格式用于变更历史，纯文本用于流转/分派等场景）';

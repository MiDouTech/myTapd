-- V33: 将 ticket_time_track.remark 从 varchar(500) 扩展为 TEXT
-- 原因：字段级变更历史以 JSON 格式存储，含 HTML 富文本（如图片标签）的字段内容
--       经序列化后可超出 500 字节限制，导致写入抛出 DataTruncation 异常。
--       与 V27 对 ticket_log.remark 的扩展保持一致。
ALTER TABLE `ticket_time_track`
    MODIFY COLUMN `remark` text COMMENT '备注（JSON 格式用于变更历史，纯文本用于流转/分派等场景）';

-- V34: 确保 ticket_log.remark 为 MEDIUMTEXT
-- 原因：V27 已将 remark 从 varchar(500) 改为 text，但部分环境可能未成功应用。
--       含富文本 HTML（图片标签等）的字段变更 JSON 可轻松超出 varchar(500) 甚至 TEXT 上限。
--       统一升级为 MEDIUMTEXT（最大 16MB）彻底消除截断风险。
ALTER TABLE `ticket_log`
    MODIFY COLUMN `remark` mediumtext COMMENT '操作备注（JSON 格式用于变更历史，纯文本用于流转/分派等场景）';

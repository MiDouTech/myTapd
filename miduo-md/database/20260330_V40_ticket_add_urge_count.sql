-- 与 ticket-platform Flyway V40 对齐：工单人工催办累计次数
ALTER TABLE `ticket`
    ADD COLUMN `urge_count` int(11) NOT NULL DEFAULT 0 COMMENT '人工催办累计次数' AFTER `closed_at`;

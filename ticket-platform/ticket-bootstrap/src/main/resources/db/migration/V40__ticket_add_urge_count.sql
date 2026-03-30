-- 工单人工催办累计次数（便于统计）
ALTER TABLE `ticket`
    ADD COLUMN `urge_count` int(11) NOT NULL DEFAULT 0 COMMENT '人工催办累计次数' AFTER `closed_at`;

-- 个人 API 密钥：累计鉴权成功次数（异步递增，列表展示）
ALTER TABLE `sys_user_api_key`
    ADD COLUMN `invocation_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '累计鉴权成功次数（异步更新）' AFTER `last_used_at`;

-- V14: 为 sys_wework_config 表增加企微回调 Token 和 AES Key 字段
-- 这两个字段用于企微回调 URL 验证和消息解密，之前只能通过环境变量配置，现改为数据库存储

ALTER TABLE `sys_wework_config`
    ADD COLUMN `callback_token` varchar(64) NOT NULL DEFAULT '' COMMENT '企微回调Token' AFTER `batch_size`,
    ADD COLUMN `callback_aes_key` varchar(64) NOT NULL DEFAULT '' COMMENT '企微回调EncodingAESKey（43位）' AFTER `callback_token`;

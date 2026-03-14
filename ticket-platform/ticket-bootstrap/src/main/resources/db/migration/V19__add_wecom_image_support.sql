-- ============================================================
-- Task024: 企微图片消息工单关联
-- V19: 新增 wecom_pending_image 暂存表 + ticket_attachment 扩展字段
-- ============================================================

-- 1. 新增企微图片消息暂存表
CREATE TABLE `wecom_pending_image` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `chat_id`      VARCHAR(100) NOT NULL COMMENT '企微群ID（私聊时为用户ID）',
    `from_user_id` VARCHAR(100) NOT NULL COMMENT '发送人企微UserID',
    `msg_id`       VARCHAR(100) NOT NULL COMMENT '企微消息ID（去重）',
    `media_id`     VARCHAR(200) DEFAULT NULL COMMENT '企微MediaId（用于API下载）',
    `pic_url`      VARCHAR(500) DEFAULT NULL COMMENT '企微图片临时预览URL',
    `qiniu_url`    VARCHAR(500) DEFAULT NULL COMMENT '七牛云持久化URL',
    `ticket_id`    BIGINT DEFAULT NULL COMMENT '关联工单ID（关联后填入）',
    `status`       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/LINKED/EXPIRED/FAILED',
    `expire_time`  DATETIME NOT NULL COMMENT '过期时间（create_time + window_minutes）',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    VARCHAR(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`    VARCHAR(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`      TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_id` (`msg_id`),
    KEY `idx_chat_user_status` (`chat_id`, `from_user_id`, `status`),
    KEY `idx_expire_time` (`expire_time`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微图片消息暂存表';

-- 2. ticket_attachment 扩展字段：来源标识 + 企微消息ID
ALTER TABLE `ticket_attachment`
    ADD COLUMN `source`       VARCHAR(30) NOT NULL DEFAULT 'WEB'
        COMMENT '附件来源：WEB/WECOM_BOT' AFTER `file_size`,
    ADD COLUMN `wecom_msg_id` VARCHAR(100) DEFAULT NULL
        COMMENT '关联的企微消息ID（仅企微来源时有值）' AFTER `source`;

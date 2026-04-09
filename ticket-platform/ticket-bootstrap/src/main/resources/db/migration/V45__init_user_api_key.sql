-- ============================================================
-- V45__init_user_api_key.sql
-- 个人 API 密钥（IDE/龙虾 Agent 调用），密钥仅哈希入库
-- ============================================================

CREATE TABLE `sys_user_api_key` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20)   NOT NULL COMMENT '所属用户ID',
    `name`            varchar(100) NOT NULL COMMENT '密钥显示名称',
    `key_prefix`      varchar(64)  NOT NULL COMMENT '密钥定位段（明文格式中的 UUID，唯一）',
    `secret_hash`     varchar(255) NOT NULL COMMENT '完整明文密钥的 BCrypt 哈希',
    `scopes`          varchar(500) DEFAULT NULL COMMENT '作用域JSON，预留',
    `status`          tinyint(4)   NOT NULL DEFAULT 1 COMMENT '状态（1:启用 0:禁用）',
    `last_used_at`    datetime     DEFAULT NULL COMMENT '最后使用时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0:否 1:是）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key_prefix` (`key_prefix`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户个人API密钥';

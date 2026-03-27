-- 米多星球 SSO 会话表：存储第三方 sessionToken（服务端管理，禁止暴露到前端）
CREATE TABLE IF NOT EXISTS `sso_session` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20)   NOT NULL                COMMENT '本地用户ID（sys_user.id）',
    `session_token`   varchar(512) NOT NULL                COMMENT '米多 sessionToken（服务端保存）',
    `miduo_user_id`   varchar(64)  DEFAULT NULL            COMMENT '米多用户ID',
    `miduo_user_name` varchar(100) DEFAULT NULL            COMMENT '米多用户名',
    `miduo_mobile`    varchar(20)  DEFAULT NULL            COMMENT '米多手机号',
    `miduo_email`     varchar(100) DEFAULT NULL            COMMENT '米多邮箱',
    `miduo_employee_no` varchar(64) DEFAULT NULL           COMMENT '米多工号',
    `expire_time`     datetime     DEFAULT NULL            COMMENT '会话过期时间',
    `revoked`         tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否已吊销（0:否 1:是）',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_token` (`session_token`(128)),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='米多SSO会话记录';

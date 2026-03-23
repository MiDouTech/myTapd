-- ============================================================
-- V30: 多人处理人 + 测试角色待分派认领/转派 + 工作流权限
-- ============================================================

-- 测试人员角色（与前端/缺陷模块 TESTER 一致）
INSERT INTO `sys_role` (`role_code`, `role_name`, `description`, `create_by`, `update_by`)
SELECT 'TESTER', '测试人员', '缺陷/工单测试侧操作：待分派认领与转派、测试信息维护', 'system', 'system'
FROM (SELECT 1 AS x) init
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_code` = 'TESTER' LIMIT 1);

CREATE TABLE IF NOT EXISTS `ticket_assignee` (
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`    bigint(20)   NOT NULL COMMENT '工单ID',
    `user_id`      bigint(20)   NOT NULL COMMENT '处理人用户ID',
    `sort_order`   int(11)      NOT NULL DEFAULT 0 COMMENT '排序（首个即主处理人，与 ticket.assignee_id 一致）',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`    varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`      tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_assignee_ticket` (`ticket_id`, `deleted`),
    KEY `idx_ticket_assignee_user` (`user_id`, `deleted`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单多人处理人';

INSERT INTO `ticket_assignee` (`ticket_id`, `user_id`, `sort_order`, `create_by`, `update_by`, `deleted`)
SELECT t.`id`, t.`assignee_id`, 0, 'system', 'system', 0
FROM `ticket` t
WHERE t.`deleted` = 0
  AND t.`assignee_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `ticket_assignee` ta
      WHERE ta.`ticket_id` = t.`id` AND ta.`user_id` = t.`assignee_id` AND ta.`deleted` = 0
  );

-- 通用工单工作流：待分派「分派受理」允许测试人员认领/分派
UPDATE `workflow` SET
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_accept","name":"分派受理","allowedRoles":["ADMIN","TICKET_ADMIN","TESTER"],"allowTransfer":true},
        {"id":"t02","from":"pending_accept","to":"processing","name":"受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t03","from":"processing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t04","from":"processing","to":"pending_verify","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t05","from":"processing","to":"pending_accept","name":"转派","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t06","from":"suspended","to":"processing","name":"恢复处理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t07","from":"pending_verify","to":"completed","name":"验收通过","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"pending_verify","to":"processing","name":"验收不通过","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t09","from":"completed","to":"pending_accept","name":"重新打开","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t10","from":"pending_assign","to":"closed","name":"关闭","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t11","from":"pending_accept","to":"closed","name":"关闭","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t12","from":"processing","to":"closed","name":"关闭","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t13","from":"suspended","to":"closed","name":"关闭","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t14","from":"pending_verify","to":"closed","name":"关闭","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 1;

-- 缺陷工单工作流：待分派「分派测试」允许测试人员认领或转派目标测试
UPDATE `workflow` SET
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_test_accept","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN","TESTER"],"allowTransfer":true},
        {"id":"t02","from":"pending_test_accept","to":"testing","name":"受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t03","from":"testing","to":"investigating","name":"进入排查","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t04","from":"investigating","to":"pending_dev_accept","name":"确认缺陷转开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t05","from":"investigating","to":"processing","name":"直接处理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t06","from":"investigating","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t07","from":"pending_dev_accept","to":"developing","name":"受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"developing","to":"processing","name":"修复提测","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t09","from":"processing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t10","from":"temp_resolved","to":"pending_verify","name":"提交验证","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t11","from":"processing","to":"pending_verify","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t12","from":"pending_verify","to":"pending_cs_confirm","name":"验收通过","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t13","from":"pending_verify","to":"developing","name":"验收不通过退回开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t14","from":"pending_cs_confirm","to":"completed","name":"客服确认关闭","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t15","from":"pending_cs_confirm","to":"testing","name":"客户仍有问题退回测试","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t16","from":"processing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t17","from":"suspended","to":"processing","name":"恢复处理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t18","from":"pending_assign","to":"closed","name":"直接关闭","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t19","from":"processing","to":"closed","name":"关闭","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 3;

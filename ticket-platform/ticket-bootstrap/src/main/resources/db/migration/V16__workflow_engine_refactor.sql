-- ============================================================
-- V15__workflow_engine_refactor.sql
-- 工单流转引擎重构：
-- 1. 统一工作流状态码为小写（与TicketStatus枚举code一致）
-- 2. 升级通用工单工作流（新增 pending_assign 初始状态、补全 closed 流转）
-- 3. 重建缺陷工单工作流（状态码统一、补全退回流转）
-- 4. 新增工作流流转日志扩展字段（流转类型区分）
-- 5. 优化分派规则表（新增 skill_tags 条件匹配支持矩阵分派）
-- 6. 新增工单流转快照表（支持流转历史追溯）
-- ============================================================

-- -----------------------------------------------------------
-- 更新通用工单工作流（ID=1）：状态码统一为小写，新增 pending_assign
-- 状态链：pending_assign → pending_accept → processing ⇄ suspended
--                                          → pending_verify → completed
--                                          (任意状态) → closed
-- -----------------------------------------------------------
UPDATE `workflow` SET
    `states` = '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_accept","name":"待受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"suspended","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE","order":4},
        {"code":"pending_verify","name":"待验收","type":"INTERMEDIATE","slaAction":"PAUSE","order":5},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":6},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":7}
    ]',
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_accept","name":"分派受理","allowedRoles":["ADMIN","TICKET_ADMIN"],"allowTransfer":true},
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

-- -----------------------------------------------------------
-- 更新审批工单工作流（ID=2）：状态码统一为小写
-- 状态链：submitted → dept_approval → executing → completed
--                   ↘ rejected → submitted（修改重提）
-- -----------------------------------------------------------
UPDATE `workflow` SET
    `states` = '[
        {"code":"submitted","name":"已提交","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"dept_approval","name":"部门审批","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":2},
        {"code":"executing","name":"执行中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":4},
        {"code":"rejected","name":"已驳回","type":"TERMINAL","slaAction":"STOP","order":5}
    ]',
    `transitions` = '[
        {"id":"t01","from":"submitted","to":"dept_approval","name":"提交审批","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t02","from":"dept_approval","to":"executing","name":"审批通过","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t03","from":"dept_approval","to":"rejected","name":"驳回","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t04","from":"rejected","to":"submitted","name":"修改重提","allowedRoles":["SUBMITTER"],"requireRemark":true,"isReturn":true},
        {"id":"t05","from":"executing","to":"completed","name":"完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 2;

-- -----------------------------------------------------------
-- 更新缺陷工单工作流（ID=3）：状态码统一为小写，补全退回标记
-- 全链路：pending_assign → pending_test_accept → testing
--         → pending_dev_accept → developing → pending_verify
--         → pending_cs_confirm → completed
-- -----------------------------------------------------------
UPDATE `workflow` SET
    `states` = '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_test_accept","name":"待测试受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"testing","name":"测试中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"pending_dev_accept","name":"待开发受理","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":4},
        {"code":"developing","name":"开发中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":5},
        {"code":"pending_verify","name":"待验收","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":6},
        {"code":"pending_cs_confirm","name":"待客服确认","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":7},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":8},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":9}
    ]',
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_test_accept","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t02","from":"pending_test_accept","to":"testing","name":"受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t03","from":"testing","to":"pending_dev_accept","name":"确认缺陷转开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t04","from":"testing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t05","from":"pending_dev_accept","to":"developing","name":"受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t06","from":"developing","to":"pending_verify","name":"修复完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t07","from":"pending_verify","to":"pending_cs_confirm","name":"验收通过","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"pending_verify","to":"developing","name":"验收不通过退回开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t09","from":"pending_cs_confirm","to":"completed","name":"客服确认关闭","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t10","from":"pending_cs_confirm","to":"testing","name":"客户仍有问题退回测试","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t11","from":"pending_assign","to":"closed","name":"直接关闭","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 3;

-- -----------------------------------------------------------
-- 同步修正存量工单状态码为小写（兼容历史数据）
-- -----------------------------------------------------------
UPDATE `ticket` SET `status` = LOWER(`status`) WHERE `deleted` = 0;

-- 修正历史遗留的别名映射
UPDATE `ticket` SET `status` = 'pending_accept'    WHERE `status` = 'pending'          AND `deleted` = 0;
UPDATE `ticket` SET `status` = 'pending_assign'    WHERE `status` = 'pending_dispatch' AND `deleted` = 0;
UPDATE `ticket` SET `status` = 'pending_test_accept' WHERE `status` = 'pending_test'   AND `deleted` = 0;
UPDATE `ticket` SET `status` = 'pending_dev_accept'  WHERE `status` = 'pending_dev'    AND `deleted` = 0;

-- -----------------------------------------------------------
-- 新增工单流转流水表（支持完整的流转历史追溯，独立于 ticket_log）
-- 相比 ticket_log，本表专为流转操作设计，含流转类型、角色、备注等
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ticket_flow_record` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`       bigint(20)   NOT NULL COMMENT '工单ID',
    `ticket_no`       varchar(50)  NOT NULL COMMENT '工单编号（冗余，便于查询）',
    `flow_type`       varchar(30)  NOT NULL COMMENT '流转类型（TRANSIT:状态流转 TRANSFER:转派 RETURN:退回 ASSIGN:分派 CLOSE:强制关闭）',
    `transition_id`   varchar(20)  DEFAULT NULL COMMENT '触发的流转规则ID（对应工作流transitions[].id）',
    `transition_name` varchar(100) DEFAULT NULL COMMENT '流转动作名称（如：受理、处理完成）',
    `from_status`     varchar(50)  NOT NULL COMMENT '流转前状态',
    `to_status`       varchar(50)  NOT NULL COMMENT '流转后状态',
    `from_assignee_id` bigint(20)  DEFAULT NULL COMMENT '流转前处理人ID',
    `to_assignee_id`  bigint(20)   DEFAULT NULL COMMENT '流转后处理人ID',
    `operator_id`     bigint(20)   NOT NULL COMMENT '操作人ID',
    `operator_role`   varchar(30)  NOT NULL COMMENT '操作时的角色（SUBMITTER/HANDLER/ADMIN/TICKET_ADMIN）',
    `remark`          varchar(500) DEFAULT NULL COMMENT '备注/原因',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_ticket_no` (`ticket_no`),
    KEY `idx_flow_type` (`flow_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单流转流水表';

-- -----------------------------------------------------------
-- 扩展分派规则表：增加 skill_match_config 支持矩阵分派
-- （矩阵分派根据工单优先级、分类、来源等匹配不同处理组/人）
-- -----------------------------------------------------------
ALTER TABLE `dispatch_rule`
    ADD COLUMN `skill_match_config` json DEFAULT NULL COMMENT '技能匹配配置（JSON格式，矩阵分派时按字段值路由）'
    AFTER `rule_config`;

-- -----------------------------------------------------------
-- 初始化矩阵分派示例规则（可选，供参考）
-- -----------------------------------------------------------
-- 矩阵分派 skill_match_config 格式示例：
-- {
--   "matchField": "priority",          -- 匹配字段（priority/source/category_id）
--   "rules": [
--     {"value": "URGENT", "groupId": 1, "userId": null},
--     {"value": "HIGH",   "groupId": 2, "userId": null},
--     {"value": "MEDIUM", "groupId": 3, "userId": null},
--     {"value": "LOW",    "groupId": 3, "userId": null}
--   ],
--   "fallbackGroupId": 3              -- 未匹配时兜底处理组
-- }

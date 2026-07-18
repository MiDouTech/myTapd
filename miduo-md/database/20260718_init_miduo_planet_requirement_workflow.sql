-- ============================================================
-- 米多星球需求状态流转工作流
-- 9 状态 / 28 流转 / 高级模式
--
-- 执行前建议：
--   1. 备份 workflow 表
--   2. 确认不存在同名工作流（可先执行下方「查重」语句）
--   3. 执行后在「管理 → 分类管理」将目标分类绑定到新工作流
--
-- 角色映射说明（矩阵中的业务角色 → 系统角色）：
--   产品/需求负责人、产品/评审人、产品/负责人 → ADMIN, TICKET_ADMIN
--   产品/提报人                         → SUBMITTER, ADMIN, TICKET_ADMIN
--   技术负责人、技术人员、处理人、调研人、方案设计人 → HANDLER, ADMIN, TICKET_ADMIN
--   产品+研发负责人、开发人员            → ADMIN, TICKET_ADMIN, HANDLER, DEVELOPER
--   开发/测试                           → HANDLER, DEVELOPER, TESTER
-- ============================================================

-- 查重（可选）
-- SELECT id, name FROM workflow WHERE name = '米多星球需求工作流' AND deleted = 0;

INSERT INTO `workflow` (
    `name`,
    `mode`,
    `description`,
    `states`,
    `transitions`,
    `is_builtin`,
    `is_active`,
    `create_by`,
    `update_by`
) VALUES (
    '米多星球需求工作流',
    'ADVANCED',
    '米多星球需求全生命周期流转：待评审→待规划→待调研→方案中→开发中→已完成；支持技术问题直转处理中、无需处理/无效终态及重新激活',
    '[
        {"code":"pending_review","name":"待评审","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_planning","name":"待规划","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"pending_research","name":"待调研","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"in_design","name":"方案中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":4},
        {"code":"developing","name":"开发中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":5},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":6},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":7},
        {"code":"no_action","name":"无需处理","type":"TERMINAL","slaAction":"STOP","order":8},
        {"code":"invalid","name":"无效","type":"TERMINAL","slaAction":"STOP","order":9}
    ]',
    '[
        {"id":"t01","from":"pending_review","to":"pending_planning","name":"评审通过","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t02","from":"pending_review","to":"pending_research","name":"需补充调研","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t03","from":"pending_review","to":"processing","name":"转技术处理","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t04","from":"pending_review","to":"no_action","name":"评审不通过","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t05","from":"pending_review","to":"invalid","name":"评审判定无效","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},

        {"id":"t06","from":"pending_planning","to":"pending_research","name":"发起调研","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t07","from":"pending_planning","to":"in_design","name":"进入方案设计","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"pending_planning","to":"developing","name":"直接开发","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t09","from":"pending_planning","to":"processing","name":"进入处理","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t10","from":"pending_planning","to":"no_action","name":"标记无需处理","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t11","from":"pending_planning","to":"invalid","name":"标记无效","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},

        {"id":"t12","from":"pending_research","to":"pending_planning","name":"退回待规划","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t13","from":"pending_research","to":"in_design","name":"调研完成转方案","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER"],"requireRemark":false},
        {"id":"t14","from":"pending_research","to":"no_action","name":"调研后不做","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER"],"requireRemark":true},
        {"id":"t15","from":"pending_research","to":"invalid","name":"调研判定无效","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER"],"requireRemark":true},

        {"id":"t16","from":"in_design","to":"pending_planning","name":"退回待规划","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t17","from":"in_design","to":"pending_research","name":"补充调研","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t18","from":"in_design","to":"developing","name":"方案通过转开发","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t19","from":"in_design","to":"no_action","name":"方案评审不通过","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},

        {"id":"t20","from":"developing","to":"in_design","name":"退回方案调整","allowedRoles":["HANDLER","DEVELOPER","TESTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t21","from":"developing","to":"processing","name":"转技术问题处理","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t22","from":"developing","to":"completed","name":"开发完成上线","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},

        {"id":"t23","from":"processing","to":"pending_planning","name":"退回待规划","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t24","from":"processing","to":"developing","name":"转需求开发","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t25","from":"processing","to":"completed","name":"处理完成","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},

        {"id":"t26","from":"completed","to":"pending_planning","name":"重新打开","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t27","from":"no_action","to":"pending_planning","name":"重新激活","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t28","from":"invalid","to":"pending_planning","name":"重新激活","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true}
    ]',
    0,
    1,
    'system',
    'system'
);

-- 查看新工作流 ID，用于分类绑定
SELECT
    id,
    name,
    mode,
    JSON_LENGTH(states)      AS state_count,
    JSON_LENGTH(transitions) AS transition_count,
    is_active,
    create_time
FROM `workflow`
WHERE `name` = '米多星球需求工作流'
  AND `deleted` = 0
ORDER BY id DESC
LIMIT 1;

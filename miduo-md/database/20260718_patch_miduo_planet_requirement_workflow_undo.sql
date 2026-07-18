-- ============================================================
-- 补丁：为已部署的「米多星球需求工作流」补充误操作回退流转
-- 28 流转 → 37 流转
--
-- 适用：已执行过初版 20260718_init_miduo_planet_requirement_workflow.sql 的环境
-- 执行前建议：SELECT id, JSON_LENGTH(transitions) FROM workflow WHERE name='米多星球需求工作流';
-- ============================================================

UPDATE `workflow`
SET
    `description` = '米多星球需求全生命周期流转：待评审→待规划→待调研→方案中→开发中→已完成；支持技术问题直转处理中、无需处理/无效终态、重新激活及各环节误操作回退',
    `transitions` = '[
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
        {"id":"t29","from":"pending_planning","to":"pending_review","name":"退回待评审","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},

        {"id":"t12","from":"pending_research","to":"pending_planning","name":"退回待规划","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t30","from":"pending_research","to":"pending_review","name":"退回待评审","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t13","from":"pending_research","to":"in_design","name":"调研完成转方案","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER"],"requireRemark":false},
        {"id":"t14","from":"pending_research","to":"no_action","name":"调研后不做","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER"],"requireRemark":true},
        {"id":"t15","from":"pending_research","to":"invalid","name":"调研判定无效","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER"],"requireRemark":true},

        {"id":"t16","from":"in_design","to":"pending_planning","name":"退回待规划","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t17","from":"in_design","to":"pending_research","name":"退回待调研","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t18","from":"in_design","to":"developing","name":"方案通过转开发","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t19","from":"in_design","to":"no_action","name":"方案评审不通过","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},

        {"id":"t20","from":"developing","to":"in_design","name":"退回方案调整","allowedRoles":["HANDLER","DEVELOPER","TESTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t31","from":"developing","to":"pending_planning","name":"退回待规划","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":true,"isReturn":true},
        {"id":"t21","from":"developing","to":"processing","name":"转技术问题处理","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t22","from":"developing","to":"completed","name":"开发完成上线","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},

        {"id":"t23","from":"processing","to":"pending_planning","name":"退回待规划","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t32","from":"processing","to":"pending_review","name":"退回待评审","allowedRoles":["ADMIN","TICKET_ADMIN","HANDLER","DEVELOPER"],"requireRemark":true,"isReturn":true},
        {"id":"t33","from":"processing","to":"developing","name":"退回开发中","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":true,"isReturn":true},
        {"id":"t24","from":"processing","to":"developing","name":"转需求开发","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},
        {"id":"t25","from":"processing","to":"completed","name":"处理完成","allowedRoles":["HANDLER","DEVELOPER"],"requireRemark":false},

        {"id":"t26","from":"completed","to":"pending_planning","name":"重新打开","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t34","from":"completed","to":"developing","name":"退回开发中","allowedRoles":["HANDLER","DEVELOPER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t35","from":"completed","to":"processing","name":"退回处理中","allowedRoles":["HANDLER","DEVELOPER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t27","from":"no_action","to":"pending_planning","name":"重新激活","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t36","from":"no_action","to":"pending_review","name":"退回待评审","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t28","from":"invalid","to":"pending_planning","name":"重新激活","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t37","from":"invalid","to":"pending_review","name":"退回待评审","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true}
    ]',
    `update_by` = 'system'
WHERE `name` = '米多星球需求工作流'
  AND `deleted` = 0;

SELECT
    id,
    name,
    JSON_LENGTH(transitions) AS transition_count,
    update_time
FROM `workflow`
WHERE `name` = '米多星球需求工作流'
  AND `deleted` = 0;

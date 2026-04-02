-- ============================================================
-- V42: 内置「告警工单流转」工作流（workflow_id=4）
-- 与通用工单工作流（id=1）states/transitions 对齐，供告警分类单独绑定；
-- 恢复事件策略见 miduo-md/workflow/告警工单流转-工作流设计.md（方案 A：仅评论）
-- ============================================================

INSERT INTO `workflow` (`id`, `name`, `mode`, `description`, `states`, `transitions`, `is_builtin`, `create_by`, `update_by`)
SELECT
    4,
    '告警工单流转',
    'SIMPLE',
    '专用于告警接入分类绑定；状态机与通用工单一致；夜莺恢复事件不自动改状态，仅系统评论',
    '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_accept","name":"待受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"suspended","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE","order":4},
        {"code":"pending_verify","name":"待验收","type":"INTERMEDIATE","slaAction":"PAUSE","order":5},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":6},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":7}
    ]',
    '[
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
    1,
    'system',
    'system'
FROM (SELECT 1 AS `_x`) AS `_ins`
WHERE NOT EXISTS (SELECT 1 FROM `workflow` WHERE `id` = 4);

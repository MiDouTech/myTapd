-- ============================================================
-- V43: 「告警工单流转」改为 OnCall 语义状态机（与通用工单分离）
-- 参考 PagerDuty incident / FlashDuty 分诊：触发→认领→处置→稳定确认→解决/抑制
-- 恢复事件仍为方案 A：仅系统评论，不自动 transition（应用层不变）
-- ============================================================

UPDATE `workflow` SET
    `description` = '告警 OnCall：待认领→处置中→待确认→已解决/已抑制/关闭；与通用工单状态码分离',
    `states` = '[
        {"code":"alert_triggered","name":"待认领","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"alert_acknowledged","name":"处置中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":2},
        {"code":"alert_stable","name":"待确认","type":"INTERMEDIATE","slaAction":"PAUSE","order":3},
        {"code":"alert_resolved","name":"已解决","type":"TERMINAL","slaAction":"STOP","order":4},
        {"code":"alert_suppressed","name":"已抑制","type":"TERMINAL","slaAction":"STOP","order":5},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":6}
    ]',
    `transitions` = '[
        {"id":"a01","from":"alert_triggered","to":"alert_acknowledged","name":"认领","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":false,"allowTransfer":true},
        {"id":"a02","from":"alert_triggered","to":"alert_acknowledged","name":"分派认领","allowedRoles":["ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":false,"allowTransfer":true},
        {"id":"a03","from":"alert_acknowledged","to":"alert_stable","name":"标记稳定","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"a04","from":"alert_acknowledged","to":"alert_suppressed","name":"抑制","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"a05","from":"alert_acknowledged","to":"alert_resolved","name":"直接解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"a06","from":"alert_stable","to":"alert_resolved","name":"确认解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","SUBMITTER"],"requireRemark":false},
        {"id":"a07","from":"alert_stable","to":"alert_acknowledged","name":"重新打开","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"a08","from":"alert_resolved","to":"alert_acknowledged","name":"重新激活","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"a09","from":"alert_triggered","to":"alert_suppressed","name":"直接抑制","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"a10","from":"alert_triggered","to":"closed","name":"关闭","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"a11","from":"alert_acknowledged","to":"closed","name":"关闭","allowedRoles":["SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"a12","from":"alert_stable","to":"closed","name":"关闭","allowedRoles":["ADMIN","TICKET_ADMIN"],"requireRemark":true}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 4 AND `name` = '告警工单流转';

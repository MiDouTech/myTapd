-- ============================================================
-- V39: 缺陷工作流去掉「待验收」节点（2026-03-29）
-- 开发处理完成 / 临时解决提交验证 均直接进入「已完成」
-- 原 pending_verify 工单回到「测试复现中」由测试继续线下验证
-- ============================================================

UPDATE `ticket` SET `status` = 'testing', `update_time` = NOW(), `update_by` = 'system'
WHERE `deleted` = 0
  AND `workflow_id` = 3
  AND LOWER(`status`) = 'pending_verify';

UPDATE `workflow` SET
    `description` = '缺陷工单：测试复现→开发解决→完成；支持临时解决与计划彻底解决时间；任意节点可挂起/非缺陷关闭；测试复现中可追加协同处理人（不改状态）',
    `states` = '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_test_accept","name":"待测试受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"testing","name":"测试复现中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"pending_dev_accept","name":"待开发受理","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":4},
        {"code":"developing","name":"开发解决中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":5},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":6},
        {"code":"temp_resolved","name":"临时解决","type":"INTERMEDIATE","slaAction":"PAUSE","order":7},
        {"code":"suspended","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE","order":8},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":9},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":10}
    ]',
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_test_accept","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN","TESTER"],"allowTransfer":true},
        {"id":"t02","from":"pending_test_accept","to":"testing","name":"测试受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t03","from":"testing","to":"pending_dev_accept","name":"确认缺陷转开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t04","from":"pending_dev_accept","to":"developing","name":"开发受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t05","from":"developing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t06","from":"developing","to":"completed","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t07","from":"temp_resolved","to":"completed","name":"验证完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"processing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t09","from":"processing","to":"completed","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"h_pa","from":"pending_assign","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":true},
        {"id":"h_pta","from":"pending_test_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_ts","from":"testing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pda","from":"pending_dev_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_dev","from":"developing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pr","from":"processing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_tr","from":"temp_resolved","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pa","from":"pending_assign","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":true},
        {"id":"c_pta","from":"pending_test_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_ts","from":"testing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pda","from":"pending_dev_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_dev","from":"developing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pr","from":"processing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_tr","from":"temp_resolved","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"r_sus","from":"suspended","to":"pending_test_accept","name":"恢复处理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"c_sus","from":"suspended","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 3;

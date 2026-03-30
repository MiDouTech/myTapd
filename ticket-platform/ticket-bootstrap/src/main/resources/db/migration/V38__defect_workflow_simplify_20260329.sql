-- ============================================================
-- V38: 缺陷工单工作流优化（2026-03-29）
-- 1. 去掉「排查中」「待客服确认」状态，合并测试受理后进入「测试复现中」(code 仍为 testing)
-- 2. 验收通过直接「已完成」；任意节点可挂起/非缺陷关闭；挂起恢复回到待测试受理
-- 3. 开发信息表增加「计划彻底解决时间」（临时解决必填）
-- 4. 历史数据：investigating -> testing；pending_cs_confirm -> pending_verify
-- ============================================================

SET @col_pf = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_bug_dev_info'
                 AND COLUMN_NAME = 'planned_full_resolve_at');
SET @sql_pf = IF(@col_pf = 0,
                 'ALTER TABLE `ticket_bug_dev_info` ADD COLUMN `planned_full_resolve_at` datetime DEFAULT NULL COMMENT ''计划彻底解决时间（临时解决时必填）'' AFTER `dev_remark`',
                 'SELECT 1');
PREPARE stmt_pf FROM @sql_pf; EXECUTE stmt_pf; DEALLOCATE PREPARE stmt_pf;

UPDATE `ticket` SET `status` = 'testing', `update_time` = NOW(), `update_by` = 'system'
WHERE `deleted` = 0 AND LOWER(`status`) = 'investigating';

UPDATE `ticket` SET `status` = 'pending_verify', `update_time` = NOW(), `update_by` = 'system'
WHERE `deleted` = 0 AND LOWER(`status`) = 'pending_cs_confirm';

UPDATE `workflow` SET
    `description` = '缺陷工单：测试复现→开发解决→验收；支持临时解决与计划彻底解决时间；任意节点可挂起/非缺陷关闭',
    `states` = '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_test_accept","name":"待测试受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"testing","name":"测试复现中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"pending_dev_accept","name":"待开发受理","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":4},
        {"code":"developing","name":"开发解决中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":5},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":6},
        {"code":"temp_resolved","name":"临时解决","type":"INTERMEDIATE","slaAction":"PAUSE","order":7},
        {"code":"pending_verify","name":"待验收","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":8},
        {"code":"suspended","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE","order":9},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":10},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":11}
    ]',
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_test_accept","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN","TESTER"],"allowTransfer":true},
        {"id":"t02","from":"pending_test_accept","to":"testing","name":"测试受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t03","from":"testing","to":"pending_dev_accept","name":"确认缺陷转开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t04","from":"pending_dev_accept","to":"developing","name":"开发受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t05","from":"developing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t06","from":"developing","to":"pending_verify","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t07","from":"temp_resolved","to":"pending_verify","name":"提交验证","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"processing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t09","from":"processing","to":"pending_verify","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t10","from":"pending_verify","to":"completed","name":"验收通过","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t11","from":"pending_verify","to":"developing","name":"验收不通过退回开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"h_pa","from":"pending_assign","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":true},
        {"id":"h_pta","from":"pending_test_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_ts","from":"testing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pda","from":"pending_dev_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_dev","from":"developing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pr","from":"processing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_tr","from":"temp_resolved","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pv","from":"pending_verify","to":"suspended","name":"挂起","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pa","from":"pending_assign","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":true},
        {"id":"c_pta","from":"pending_test_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_ts","from":"testing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pda","from":"pending_dev_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_dev","from":"developing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pr","from":"processing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_tr","from":"temp_resolved","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pv","from":"pending_verify","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"r_sus","from":"suspended","to":"pending_test_accept","name":"恢复处理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"c_sus","from":"suspended","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true}
    ]',
    `update_by` = 'system',
    `update_time` = NOW()
WHERE `id` = 3;

-- ============================================================
-- V25__defect_management_v22_alignment.sql
-- 缺陷管理 V2.2 首批基线对齐：
-- 1. ticket_bug_test_info.severity_level 统一到 P0~P4
-- 2. 缺陷工单工作流补齐 investigating / temp_resolved 状态
-- ============================================================

-- -----------------------------------------------------------
-- 1) 缺陷等级历史值映射（兼容 FATAL/CRITICAL/NORMAL/MINOR）
-- -----------------------------------------------------------
UPDATE `ticket_bug_test_info`
SET `severity_level` = 'P0'
WHERE `deleted` = 0 AND UPPER(`severity_level`) = 'FATAL';

UPDATE `ticket_bug_test_info`
SET `severity_level` = 'P1'
WHERE `deleted` = 0 AND UPPER(`severity_level`) = 'CRITICAL';

UPDATE `ticket_bug_test_info`
SET `severity_level` = 'P2'
WHERE `deleted` = 0 AND UPPER(`severity_level`) = 'NORMAL';

UPDATE `ticket_bug_test_info`
SET `severity_level` = 'P3'
WHERE `deleted` = 0 AND UPPER(`severity_level`) = 'MINOR';

UPDATE `ticket_bug_test_info`
SET `severity_level` = UPPER(`severity_level`)
WHERE `deleted` = 0
  AND `severity_level` IS NOT NULL
  AND UPPER(`severity_level`) REGEXP '^P[0-4]$';

ALTER TABLE `ticket_bug_test_info`
    MODIFY COLUMN `severity_level` varchar(20) DEFAULT NULL
    COMMENT '缺陷等级（P0:致命 P1:严重 P2:一般 P3:轻微 P4:建议）';

-- -----------------------------------------------------------
-- 2) 缺陷工单工作流（ID=3）状态机补齐
-- -----------------------------------------------------------
UPDATE `workflow` SET
    `states` = '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_test_accept","name":"待测试受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"testing","name":"测试中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"investigating","name":"排查中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":4},
        {"code":"pending_dev_accept","name":"待开发受理","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":5},
        {"code":"developing","name":"开发中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":6},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":7},
        {"code":"temp_resolved","name":"临时解决","type":"INTERMEDIATE","slaAction":"PAUSE","order":8},
        {"code":"pending_verify","name":"待验收","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":9},
        {"code":"pending_cs_confirm","name":"待客服确认","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":10},
        {"code":"suspended","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE","order":11},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":12},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":13}
    ]',
    `transitions` = '[
        {"id":"t01","from":"pending_assign","to":"pending_test_accept","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN"],"allowTransfer":true},
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

-- ============================================================
-- 复制「缺陷工单工作流」(id=3)，在「待测试受理」前插入「待客服受理」
-- 基准版本：V39（10 状态 / 25 流转）
-- 新工作流：11 状态 / 28 流转
--
-- 执行前建议：
--   1. 备份 workflow 表
--   2. 确认 id=3 的缺陷工单工作流未被 UI 大幅改动（可先 SELECT 核对）
--   3. 执行后在「管理 → 分类管理」将目标分类绑定到新工作流
--
-- 注意：状态码 pending_cs_accept 需同步后端枚举与前端展示映射，否则界面可能显示原始 code
-- ============================================================

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
    '缺陷工单工作流（含客服受理）',
    'ADVANCED',
    '基于缺陷工单工作流复制：待分派→待客服受理→待测试受理→测试复现→开发解决→完成；支持挂起/非缺陷关闭',
    '[
        {"code":"pending_assign","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"pending_cs_accept","name":"待客服受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":2},
        {"code":"pending_test_accept","name":"待测试受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE","order":3},
        {"code":"testing","name":"测试复现中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":4},
        {"code":"pending_dev_accept","name":"待开发受理","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":5},
        {"code":"developing","name":"开发解决中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":6},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":7},
        {"code":"temp_resolved","name":"临时解决","type":"INTERMEDIATE","slaAction":"PAUSE","order":8},
        {"code":"suspended","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE","order":9},
        {"code":"completed","name":"已完成","type":"TERMINAL","slaAction":"STOP","order":10},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":11}
    ]',
    '[
        {"id":"t01","from":"pending_assign","to":"pending_cs_accept","name":"分派客服","allowedRoles":["ADMIN","TICKET_ADMIN","CUSTOMER_SERVICE"],"allowTransfer":true},
        {"id":"t02","from":"pending_cs_accept","to":"pending_test_accept","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN","CUSTOMER_SERVICE","TESTER"],"allowTransfer":true},
        {"id":"t03","from":"pending_test_accept","to":"testing","name":"测试受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t04","from":"testing","to":"pending_dev_accept","name":"确认缺陷转开发","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"allowTransfer":true},
        {"id":"t05","from":"pending_dev_accept","to":"developing","name":"开发受理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t06","from":"developing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t07","from":"developing","to":"completed","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t08","from":"temp_resolved","to":"completed","name":"验证完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t09","from":"processing","to":"temp_resolved","name":"临时解决","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t10","from":"processing","to":"completed","name":"处理完成","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"h_pa","from":"pending_assign","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":true},
        {"id":"h_pcs","from":"pending_cs_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","CUSTOMER_SERVICE"],"requireRemark":true},
        {"id":"h_pta","from":"pending_test_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_ts","from":"testing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pda","from":"pending_dev_accept","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_dev","from":"developing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_pr","from":"processing","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"h_tr","from":"temp_resolved","to":"suspended","name":"挂起","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pa","from":"pending_assign","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","TESTER"],"requireRemark":true},
        {"id":"c_pcs","from":"pending_cs_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN","CUSTOMER_SERVICE"],"requireRemark":true},
        {"id":"c_pta","from":"pending_test_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_ts","from":"testing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pda","from":"pending_dev_accept","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_dev","from":"developing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_pr","from":"processing","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"c_tr","from":"temp_resolved","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"r_sus","from":"suspended","to":"pending_cs_accept","name":"恢复处理","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"c_sus","from":"suspended","to":"closed","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true}
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
    JSON_LENGTH(states)      AS state_count,
    JSON_LENGTH(transitions) AS transition_count,
    is_active
FROM `workflow`
WHERE `name` = '缺陷工单工作流（含客服受理）'
  AND `deleted` = 0
ORDER BY id DESC
LIMIT 1;

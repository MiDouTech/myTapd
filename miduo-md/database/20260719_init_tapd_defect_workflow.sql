-- ============================================================
-- TAPD 缺陷状态流转工作流
-- 6 状态 / 8 流转 / 高级模式
--
-- 主流转路径：
--   新建 → 处理中 → 已修复 → 待验证 → 已关闭
--   新建 → 已拒绝（开发直接拒绝）
--   待验证 → 处理中（验证不通过打回）
--   已关闭 / 已拒绝 → 新建（重新激活）
--
-- 执行前建议：
--   1. 备份 workflow 表
--   2. 确认不存在同名工作流（可先执行下方「查重」语句）
--   3. 执行后在「管理 → 分类管理」将目标分类绑定到新工作流
--
-- 角色映射说明（矩阵中的业务角色 → 系统角色）：
--   开发人员           → HANDLER, DEVELOPER, ADMIN, TICKET_ADMIN
--   测试人员           → TESTER, HANDLER, ADMIN, TICKET_ADMIN
--   提交人             → SUBMITTER, ADMIN, TICKET_ADMIN
--   开发/测试          → HANDLER, DEVELOPER, TESTER, ADMIN, TICKET_ADMIN
--   测试/开发（重开）  → TESTER, HANDLER, DEVELOPER, SUBMITTER, ADMIN, TICKET_ADMIN
--
-- 状态码说明（需同步后端 TicketStatus 枚举与前端 ticket-status.ts）：
--   new_created   新建
--   processing    处理中
--   fixed         已修复
--   pending_verify 待验证
--   closed        已关闭
--   rejected      已拒绝
-- ============================================================

-- 查重（可选）
-- SELECT id, name FROM workflow WHERE name = 'TAPD缺陷工作流' AND deleted = 0;

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
    'TAPD缺陷工作流',
    'ADVANCED',
    'TAPD 缺陷精简流转：新建→处理中→已修复→待验证→已关闭；开发可直接拒绝；支持验证打回与终态重新激活',
    '[
        {"code":"new_created","name":"新建","type":"INITIAL","slaAction":"START_RESPONSE","order":1},
        {"code":"processing","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":2},
        {"code":"fixed","name":"已修复","type":"INTERMEDIATE","slaAction":"START_RESOLVE","order":3},
        {"code":"pending_verify","name":"待验证","type":"INTERMEDIATE","slaAction":"PAUSE","order":4},
        {"code":"closed","name":"已关闭","type":"TERMINAL","slaAction":"STOP","order":5},
        {"code":"rejected","name":"已拒绝","type":"TERMINAL","slaAction":"STOP","order":6}
    ]',
    '[
        {"id":"t01","from":"new_created","to":"processing","name":"确认并开始处理","allowedRoles":["HANDLER","DEVELOPER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t02","from":"new_created","to":"rejected","name":"直接拒绝","allowedRoles":["HANDLER","DEVELOPER","ADMIN","TICKET_ADMIN"],"requireRemark":true},
        {"id":"t03","from":"processing","to":"fixed","name":"修复完成","allowedRoles":["HANDLER","DEVELOPER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t04","from":"fixed","to":"pending_verify","name":"提交验证","allowedRoles":["HANDLER","DEVELOPER","TESTER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t05","from":"pending_verify","to":"processing","name":"验证不通过，重新打开","allowedRoles":["TESTER","HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t06","from":"pending_verify","to":"closed","name":"验证通过","allowedRoles":["TESTER","HANDLER","ADMIN","TICKET_ADMIN"],"requireRemark":false},
        {"id":"t07","from":"closed","to":"new_created","name":"重新激活","allowedRoles":["TESTER","HANDLER","DEVELOPER","SUBMITTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true},
        {"id":"t08","from":"rejected","to":"new_created","name":"重新激活","allowedRoles":["SUBMITTER","TESTER","ADMIN","TICKET_ADMIN"],"requireRemark":true,"isReturn":true}
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
WHERE `name` = 'TAPD缺陷工作流'
  AND `deleted` = 0
ORDER BY id DESC
LIMIT 1;

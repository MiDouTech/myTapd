-- ============================================================
-- 米多工单平台（ticket_platform）历史测试数据清理脚本
-- 生成依据：ticket-platform Flyway 迁移表结构
-- ============================================================
--
-- 【作用】物理清空业务流水与测试工单数据，便于正式投产前库容“归零”。
-- 【保留】部门/用户/角色、工作流与 SLA 定义、工单分类与模板、处理组、
--         分派规则、企微/Webhook 配置、字典与模块主数据、system_config 等。
--
-- 【执行前必做】
--   1. 全库备份（mysqldump 或快照）
--   2. 确认库名（默认 ticket_platform）
--   3. 建议在维护窗口执行；执行后建议清空 Redis 中与登录/会话相关的键（若使用）
--
-- 【用法】
--   mysql -h<host> -u<user> -p < 本文件.sql
--   或在客户端中选中 USE 以下库名后执行全文
-- ============================================================

USE `ticket_platform`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- ----------------------------------------------------------------
-- 1) 与工单/简报相关的通知、Webhook、企微消息与待处理图片
-- ----------------------------------------------------------------
TRUNCATE TABLE `notification`;
TRUNCATE TABLE `webhook_dispatch_log`;
TRUNCATE TABLE `wecom_bot_message_log`;
TRUNCATE TABLE `wecom_pending_image`;

-- ----------------------------------------------------------------
-- 2) 工单子表（先于 ticket 主表）
-- ----------------------------------------------------------------
TRUNCATE TABLE `ticket_assignee`;
TRUNCATE TABLE `ticket_comment`;
TRUNCATE TABLE `ticket_log`;
TRUNCATE TABLE `ticket_attachment`;
TRUNCATE TABLE `ticket_follower`;
TRUNCATE TABLE `ticket_custom_field`;
TRUNCATE TABLE `ticket_flow_record`;
TRUNCATE TABLE `sla_timer`;
TRUNCATE TABLE `ticket_time_track`;
TRUNCATE TABLE `ticket_node_duration`;
TRUNCATE TABLE `ticket_bug_info`;
TRUNCATE TABLE `ticket_bug_test_info`;
TRUNCATE TABLE `ticket_bug_dev_info`;

-- ----------------------------------------------------------------
-- 3) 工单主表
-- ----------------------------------------------------------------
TRUNCATE TABLE `ticket`;

-- ----------------------------------------------------------------
-- 4) Bug 简报及关联（简报内再关联工单 ID，与工单一并清空）
-- ----------------------------------------------------------------
TRUNCATE TABLE `bug_report_log`;
TRUNCATE TABLE `bug_report_attachment`;
TRUNCATE TABLE `bug_report_responsible`;
TRUNCATE TABLE `bug_report_ticket`;
TRUNCATE TABLE `bug_report`;

-- ----------------------------------------------------------------
-- 5) 会话、操作审计、看板布局、企微同步日志（运行期流水，非配置）
-- ----------------------------------------------------------------
TRUNCATE TABLE `sso_session`;
TRUNCATE TABLE `sys_operation_log`;
TRUNCATE TABLE `dashboard_user_layout`;
TRUNCATE TABLE `sys_sync_log`;

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;

-- ============================================================
-- 可选：若你希望连「分类、模板、处理组、分派规则、模块字典」等一并重置，
-- 请先确认可接受丢失全部配置，再取消下面注释单独执行（不要与上面同一事务混用未测场景）。
-- ============================================================
/*
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `dispatch_rule`;
TRUNCATE TABLE `handler_group_member`;
TRUNCATE TABLE `handler_group`;
TRUNCATE TABLE `ticket_template`;
TRUNCATE TABLE `ticket_category`;
TRUNCATE TABLE `ticket_module`;
TRUNCATE TABLE `ticket_assignment_rule`;
TRUNCATE TABLE `ticket_user_role`;
TRUNCATE TABLE `wecom_group_binding`;
TRUNCATE TABLE `webhook_config`;
TRUNCATE TABLE `wecom_nlp_keyword`;
TRUNCATE TABLE `dict_logic_cause`;
TRUNCATE TABLE `dict_defect_category`;
TRUNCATE TABLE `dict_project`;
SET FOREIGN_KEY_CHECKS = 1;
*/

SELECT 'ticket_platform 历史测试数据清理完成（工单/简报/日志/通知等已清空，组织与配置表保留）。' AS result;

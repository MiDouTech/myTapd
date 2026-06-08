-- ============================================================
-- V61: 告警心跳规则按项目编号预置映射
-- 说明：夜莺 HTTP 媒介无法稳定传递告警接收人，先预置 YY 项目规则，处理人在后台规则映射页配置。
-- ============================================================

ALTER TABLE `alert_rule_mapping`
    ADD COLUMN `assignee_ids` varchar(500) NOT NULL DEFAULT '' COMMENT '固定处理人用户ID列表，逗号分隔，首个同步到assignee_id' AFTER `assignee_id`;

UPDATE `alert_rule_mapping`
SET `assignee_ids` = CAST(`assignee_id` AS CHAR)
WHERE `assignee_id` IS NOT NULL
  AND (`assignee_ids` IS NULL OR `assignee_ids` = '');

SET @alert_category_id := COALESCE(
    (SELECT `category_id`
     FROM `alert_rule_mapping`
     WHERE `deleted` = 0
       AND `rule_name` = '监控中心异常告警-心跳超过10分钟'
     ORDER BY `id`
     LIMIT 1),
    (SELECT `id`
     FROM `ticket_category`
     WHERE `deleted` = 0
       AND `name` = '监控告警'
     ORDER BY `id`
     LIMIT 1),
    (SELECT `id`
     FROM `ticket_category`
     WHERE `deleted` = 0
     ORDER BY `id`
     LIMIT 1)
);

CREATE TEMPORARY TABLE `tmp_alert_project_assignment` (
    `project_name` varchar(50) NOT NULL,
    `project_code` varchar(20) NOT NULL,
    `rule_name` varchar(200) NOT NULL,
    PRIMARY KEY (`project_code`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_alert_project_assignment` (`project_name`, `project_code`, `rule_name`) VALUES
('智能营销', 'YY001', '监控中心异常告警-心跳超过10分钟(YY001）'),
('社交云店', 'YY002', '监控中心异常告警-心跳超过10分钟(YY002）'),
('DCRM', 'YY003', '监控中心异常告警-心跳超过10分钟(YY003）'),
('防窜物流', 'YY004', '监控中心异常告警-心跳超过10分钟(YY004）'),
('万能溯源', 'YY005', '监控中心异常告警-心跳超过10分钟(YY005）'),
('微商城', 'YY006', '监控中心异常告警-心跳超过10分钟(YY006）'),
('智慧零售', 'YY007', '监控中心异常告警-心跳超过10分钟(YY007）'),
('商户后台', 'YY008', '监控中心异常告警-心跳超过10分钟(YY008）'),
('企业微信', 'YY009', '监控中心异常告警-心跳超过10分钟(YY009）'),
('互动营销', 'YY010', '监控中心异常告警-心跳超过10分钟(YY010）'),
('外勤管理', 'YY011', '监控中心异常告警-心跳超过10分钟(YY011）'),
('米多星球', 'YY012', '监控中心异常告警-心跳超过10分钟(YY012）');

INSERT INTO `alert_rule_mapping` (
    `rule_name`,
    `match_mode`,
    `category_id`,
    `priority_p1`,
    `priority_p2`,
    `priority_p3`,
    `assignee_id`,
    `assignee_ids`,
    `dedup_window_minutes`,
    `enabled`,
    `create_by`,
    `update_by`
)
SELECT
    t.`rule_name`,
    'EXACT',
    @alert_category_id,
    'urgent',
    'high',
    'medium',
    NULL,
    '',
    30,
    1,
    'system',
    'system'
FROM `tmp_alert_project_assignment` t
WHERE @alert_category_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `alert_rule_mapping` m
      WHERE m.`deleted` = 0
        AND REPLACE(REPLACE(m.`rule_name`, '（', '('), '）', ')')
            = REPLACE(REPLACE(t.`rule_name`, '（', '('), '）', ')')
  );

UPDATE `alert_rule_mapping` m
JOIN `tmp_alert_project_assignment` s
    ON REPLACE(REPLACE(m.`rule_name`, '（', '('), '）', ')')
        = REPLACE(REPLACE(s.`rule_name`, '（', '('), '）', ')')
SET
    m.`match_mode` = 'EXACT',
    m.`category_id` = COALESCE(@alert_category_id, m.`category_id`),
    m.`priority_p1` = 'urgent',
    m.`priority_p2` = 'high',
    m.`priority_p3` = 'medium',
    m.`dedup_window_minutes` = 30,
    m.`enabled` = 1,
    m.`update_by` = 'system',
    m.`update_time` = NOW()
WHERE @alert_category_id IS NOT NULL
  AND m.`deleted` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_alert_project_assignment`;

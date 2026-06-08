-- ============================================================
-- V61: 告警心跳规则按项目编号固定分派
-- 说明：夜莺 HTTP 媒介无法稳定传递告警接收人，改为按规则名中的 YY 项目编号固定分派。
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
    `assignee_names` varchar(200) NOT NULL,
    PRIMARY KEY (`project_code`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_alert_project_assignment` (`project_name`, `project_code`, `rule_name`, `assignee_names`) VALUES
('智能营销', 'YY001', '监控中心异常告警-心跳超过10分钟(YY001）', '周满'),
('社交云店', 'YY002', '监控中心异常告警-心跳超过10分钟(YY002）', '宇凡'),
('DCRM', 'YY003', '监控中心异常告警-心跳超过10分钟(YY003）', '宇凡'),
('防窜物流', 'YY004', '监控中心异常告警-心跳超过10分钟(YY004）', '仁平,舒怀'),
('万能溯源', 'YY005', '监控中心异常告警-心跳超过10分钟(YY005）', '王忠'),
('微商城', 'YY006', '监控中心异常告警-心跳超过10分钟(YY006）', '宇凡'),
('智慧零售', 'YY007', '监控中心异常告警-心跳超过10分钟(YY007）', '宇凡'),
('商户后台', 'YY008', '监控中心异常告警-心跳超过10分钟(YY008）', '宇凡'),
('企业微信', 'YY009', '监控中心异常告警-心跳超过10分钟(YY009）', '宇凡'),
('互动营销', 'YY010', '监控中心异常告警-心跳超过10分钟(YY010）', '卫杰'),
('外勤管理', 'YY011', '监控中心异常告警-心跳超过10分钟(YY011）', '卫杰'),
('米多星球', 'YY012', '监控中心异常告警-心跳超过10分钟(YY012）', '志涛');

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
    CAST(SUBSTRING_INDEX(GROUP_CONCAT(u.`id` ORDER BY FIND_IN_SET(u.`name`, t.`assignee_names`)), ',', 1) AS UNSIGNED),
    COALESCE(GROUP_CONCAT(u.`id` ORDER BY FIND_IN_SET(u.`name`, t.`assignee_names`)), ''),
    30,
    1,
    'system',
    'system'
FROM `tmp_alert_project_assignment` t
LEFT JOIN `sys_user` u
    ON FIND_IN_SET(u.`name`, t.`assignee_names`) > 0
   AND u.`deleted` = 0
   AND u.`account_status` = 1
WHERE @alert_category_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `alert_rule_mapping` m
      WHERE m.`deleted` = 0
        AND REPLACE(REPLACE(m.`rule_name`, '（', '('), '）', ')')
            = REPLACE(REPLACE(t.`rule_name`, '（', '('), '）', ')')
  )
GROUP BY t.`project_code`, t.`rule_name`;

UPDATE `alert_rule_mapping` m
JOIN (
    SELECT
        t.`rule_name`,
        CAST(SUBSTRING_INDEX(GROUP_CONCAT(u.`id` ORDER BY FIND_IN_SET(u.`name`, t.`assignee_names`)), ',', 1) AS UNSIGNED) AS `assignee_id`,
        COALESCE(GROUP_CONCAT(u.`id` ORDER BY FIND_IN_SET(u.`name`, t.`assignee_names`)), '') AS `assignee_ids`
    FROM `tmp_alert_project_assignment` t
    LEFT JOIN `sys_user` u
        ON FIND_IN_SET(u.`name`, t.`assignee_names`) > 0
       AND u.`deleted` = 0
       AND u.`account_status` = 1
    GROUP BY t.`project_code`, t.`rule_name`
) s
    ON REPLACE(REPLACE(m.`rule_name`, '（', '('), '）', ')')
        = REPLACE(REPLACE(s.`rule_name`, '（', '('), '）', ')')
SET
    m.`match_mode` = 'EXACT',
    m.`category_id` = COALESCE(@alert_category_id, m.`category_id`),
    m.`priority_p1` = 'urgent',
    m.`priority_p2` = 'high',
    m.`priority_p3` = 'medium',
    m.`assignee_id` = COALESCE(s.`assignee_id`, m.`assignee_id`),
    m.`assignee_ids` = CASE WHEN s.`assignee_ids` <> '' THEN s.`assignee_ids` ELSE m.`assignee_ids` END,
    m.`dedup_window_minutes` = 30,
    m.`enabled` = 1,
    m.`update_by` = 'system',
    m.`update_time` = NOW()
WHERE @alert_category_id IS NOT NULL
  AND m.`deleted` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_alert_project_assignment`;

-- ============================================================
-- 20260511_2_backfill_missing_start_process_nodes.sql
-- 目的：补齐历史中由 START_PROCESS 跨状态流转造成的缺失节点行
-- 场景：如 pending_test_accept -> testing、pending_dev_accept -> developing
-- 适用：MySQL 8.0+
-- 说明：建议先执行 20260511_1 脚本，再执行本脚本
-- ============================================================

START TRANSACTION;

-- ------------------------------------------------------------
-- Step 0: 预览候选（哪些 START_PROCESS 事件缺少目标节点）
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_missing_start_process_node_raw;
CREATE TEMPORARY TABLE tmp_missing_start_process_node_raw AS
SELECT
    t.id AS track_id,
    t.ticket_id,
    t.from_status,
    t.to_status AS node_name,
    t.to_user_id,
    t.`timestamp` AS arrive_at,
    (
        SELECT n_prev.assignee_id
        FROM ticket_node_duration n_prev
        WHERE n_prev.deleted = 0
          AND n_prev.ticket_id = t.ticket_id
          AND n_prev.arrive_at IS NOT NULL
          AND n_prev.arrive_at <= t.`timestamp`
        ORDER BY n_prev.arrive_at DESC, n_prev.id DESC
        LIMIT 1
    ) AS prev_assignee_id,
    (
        SELECT n_prev.assignee_role
        FROM ticket_node_duration n_prev
        WHERE n_prev.deleted = 0
          AND n_prev.ticket_id = t.ticket_id
          AND n_prev.arrive_at IS NOT NULL
          AND n_prev.arrive_at <= t.`timestamp`
        ORDER BY n_prev.arrive_at DESC, n_prev.id DESC
        LIMIT 1
    ) AS prev_assignee_role,
    (
        SELECT MIN(r.`timestamp`)
        FROM ticket_time_track r
        WHERE r.deleted = 0
          AND r.ticket_id = t.ticket_id
          AND r.action = 'READ'
          AND r.to_status = t.to_status
          AND r.`timestamp` >= t.`timestamp`
    ) AS first_read_at_raw,
    (
        SELECT MIN(n2.arrive_at)
        FROM ticket_node_duration n2
        WHERE n2.deleted = 0
          AND n2.ticket_id = t.ticket_id
          AND n2.arrive_at IS NOT NULL
          AND n2.arrive_at > t.`timestamp`
    ) AS next_arrive_at,
    (
        SELECT MIN(t2.`timestamp`)
        FROM ticket_time_track t2
        WHERE t2.deleted = 0
          AND t2.ticket_id = t.ticket_id
          AND t2.from_status = t.to_status
          AND t2.to_status IS NOT NULL
          AND t2.to_status <> t2.from_status
          AND t2.`timestamp` > t.`timestamp`
    ) AS transition_leave_at
FROM ticket_time_track t
WHERE t.deleted = 0
  AND t.action = 'START_PROCESS'
  AND t.from_status IS NOT NULL
  AND t.to_status IS NOT NULL
  AND t.from_status <> t.to_status
  AND NOT EXISTS (
      SELECT 1
      FROM ticket_node_duration n
      WHERE n.deleted = 0
        AND n.ticket_id = t.ticket_id
        AND n.node_name = t.to_status
        AND n.arrive_at IS NOT NULL
        AND ABS(TIMESTAMPDIFF(SECOND, n.arrive_at, t.`timestamp`)) <= 60
  );

DROP TEMPORARY TABLE IF EXISTS tmp_missing_start_process_node_final;
CREATE TEMPORARY TABLE tmp_missing_start_process_node_final AS
SELECT
    r.track_id,
    r.ticket_id,
    r.from_status,
    r.node_name,
    r.arrive_at,
    COALESCE(r.to_user_id, r.prev_assignee_id, tk.assignee_id) AS assignee_id_fill,
    r.prev_assignee_role AS assignee_role_fill,
    CASE
        WHEN r.next_arrive_at IS NOT NULL AND r.transition_leave_at IS NOT NULL THEN LEAST(r.next_arrive_at, r.transition_leave_at)
        WHEN r.next_arrive_at IS NOT NULL THEN r.next_arrive_at
        WHEN r.transition_leave_at IS NOT NULL THEN r.transition_leave_at
        ELSE NULL
    END AS leave_at_fill,
    r.first_read_at_raw
FROM tmp_missing_start_process_node_raw r
LEFT JOIN ticket tk
    ON tk.id = r.ticket_id
   AND tk.deleted = 0
WHERE r.arrive_at IS NOT NULL;

SELECT COUNT(1) AS missing_node_rows_to_insert
FROM tmp_missing_start_process_node_final;

SELECT
    f.ticket_id,
    f.track_id,
    f.from_status,
    f.node_name,
    f.arrive_at,
    f.leave_at_fill
FROM tmp_missing_start_process_node_final f
ORDER BY f.ticket_id, f.arrive_at
LIMIT 200;

-- ------------------------------------------------------------
-- Step 1: 插入缺失节点
-- 规则：
--  - start_process_at = arrive_at（START_PROCESS 本身）
--  - first_read_at 仅在 <= leave_at（或 leave_at 为空）时写入
--  - leave_at / duration 按候选值补齐
-- ------------------------------------------------------------
INSERT INTO ticket_node_duration (
    ticket_id,
    node_name,
    assignee_id,
    assignee_role,
    arrive_at,
    first_read_at,
    start_process_at,
    leave_at,
    wait_duration_sec,
    process_duration_sec,
    total_duration_sec,
    create_time,
    update_time,
    create_by,
    update_by,
    deleted
)
SELECT
    f.ticket_id,
    f.node_name,
    f.assignee_id_fill,
    f.assignee_role_fill,
    f.arrive_at,
    CASE
        WHEN f.first_read_at_raw IS NOT NULL
             AND (f.leave_at_fill IS NULL OR f.first_read_at_raw <= f.leave_at_fill)
            THEN f.first_read_at_raw
        ELSE NULL
    END AS first_read_at_fill,
    f.arrive_at AS start_process_at_fill,
    f.leave_at_fill,
    CASE
        WHEN f.first_read_at_raw IS NOT NULL
             AND (f.leave_at_fill IS NULL OR f.first_read_at_raw <= f.leave_at_fill)
            THEN GREATEST(TIMESTAMPDIFF(SECOND, f.arrive_at, f.first_read_at_raw), 0)
        ELSE NULL
    END AS wait_duration_sec_fill,
    CASE
        WHEN f.leave_at_fill IS NOT NULL
            THEN GREATEST(TIMESTAMPDIFF(SECOND, f.arrive_at, f.leave_at_fill), 0)
        ELSE NULL
    END AS process_duration_sec_fill,
    CASE
        WHEN f.leave_at_fill IS NOT NULL
            THEN GREATEST(TIMESTAMPDIFF(SECOND, f.arrive_at, f.leave_at_fill), 0)
        ELSE NULL
    END AS total_duration_sec_fill,
    NOW(),
    NOW(),
    'backfill_start_process_node_20260511_2',
    'backfill_start_process_node_20260511_2',
    0
FROM tmp_missing_start_process_node_final f
WHERE NOT EXISTS (
    SELECT 1
    FROM ticket_node_duration n
    WHERE n.deleted = 0
      AND n.ticket_id = f.ticket_id
      AND n.node_name = f.node_name
      AND n.arrive_at IS NOT NULL
      AND ABS(TIMESTAMPDIFF(SECOND, n.arrive_at, f.arrive_at)) <= 60
);

SELECT ROW_COUNT() AS inserted_missing_node_rows;

-- ------------------------------------------------------------
-- Step 2: 校验（重点看 testing/developing 是否已补出）
-- ------------------------------------------------------------
SELECT
    node_name,
    COUNT(1) AS node_count
FROM ticket_node_duration
WHERE deleted = 0
  AND node_name IN ('TESTING', 'DEVELOPING')
GROUP BY node_name;

-- 仍存在“有 START_PROCESS 跨状态记录，但目标节点仍缺失”的数量
SELECT
    COUNT(1) AS remain_missing_start_process_target_node
FROM ticket_time_track t
WHERE t.deleted = 0
  AND t.action = 'START_PROCESS'
  AND t.from_status IS NOT NULL
  AND t.to_status IS NOT NULL
  AND t.from_status <> t.to_status
  AND NOT EXISTS (
      SELECT 1
      FROM ticket_node_duration n
      WHERE n.deleted = 0
        AND n.ticket_id = t.ticket_id
        AND n.node_name = t.to_status
        AND n.arrive_at IS NOT NULL
        AND ABS(TIMESTAMPDIFF(SECOND, n.arrive_at, t.`timestamp`)) <= 60
  );

COMMIT;

-- 如需演练回滚，请将 COMMIT 改为 ROLLBACK 后执行。

-- ============================================================
-- 20260511_1_backfill_ticket_node_duration_leave_at.sql
-- 目的：一次性回填历史节点耗时中遗留的 leave_at / start_process_at / duration 字段
-- 适用：MySQL 8.0+
-- 执行建议：先在测试环境验证，再在生产环境执行
-- ============================================================

START TRANSACTION;

-- ------------------------------------------------------------
-- Step 0: 回填前总览（影响规模）
-- ------------------------------------------------------------
SELECT
    COUNT(1) AS open_node_count
FROM ticket_node_duration n
WHERE n.deleted = 0
  AND n.leave_at IS NULL;

-- ------------------------------------------------------------
-- Step 1: 识别候选 leave_at
-- 规则：
--  1) 优先取“下一节点的到达时间”
--  2) 若存在状态流转事件时间（from_status = 当前 node_name），取两者较早值
--  3) 仅回填 leave_at 为空且能计算出候选时间的历史节点
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_node_leave_backfill_raw;
CREATE TEMPORARY TABLE tmp_node_leave_backfill_raw AS
SELECT
    n.id,
    n.ticket_id,
    n.node_name,
    n.arrive_at,
    n.first_read_at,
    n.start_process_at,
    (
        SELECT MIN(n2.arrive_at)
        FROM ticket_node_duration n2
        WHERE n2.deleted = 0
          AND n2.ticket_id = n.ticket_id
          AND n2.arrive_at IS NOT NULL
          AND (
                n.arrive_at IS NULL
                OR n2.arrive_at > n.arrive_at
                OR (n2.arrive_at = n.arrive_at AND n2.id > n.id)
              )
    ) AS next_arrive_at,
    (
        SELECT MIN(t.`timestamp`)
        FROM ticket_time_track t
        WHERE t.deleted = 0
          AND t.ticket_id = n.ticket_id
          AND t.from_status = n.node_name
          AND t.to_status IS NOT NULL
          AND t.to_status <> t.from_status
          AND (n.arrive_at IS NULL OR t.`timestamp` >= n.arrive_at)
    ) AS transition_at
FROM ticket_node_duration n
WHERE n.deleted = 0
  AND n.leave_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_node_leave_backfill_final;
CREATE TEMPORARY TABLE tmp_node_leave_backfill_final AS
SELECT
    r.id,
    r.ticket_id,
    r.node_name,
    r.arrive_at,
    r.first_read_at,
    r.start_process_at,
    CASE
        WHEN r.next_arrive_at IS NOT NULL AND r.transition_at IS NOT NULL THEN LEAST(r.next_arrive_at, r.transition_at)
        WHEN r.next_arrive_at IS NOT NULL THEN r.next_arrive_at
        WHEN r.transition_at IS NOT NULL THEN r.transition_at
        ELSE NULL
    END AS leave_at_fill
FROM tmp_node_leave_backfill_raw r
HAVING leave_at_fill IS NOT NULL
   AND (arrive_at IS NULL OR leave_at_fill >= arrive_at);

-- ------------------------------------------------------------
-- Step 2: 回填预览（执行 UPDATE 前建议先查看）
-- ------------------------------------------------------------
SELECT
    COUNT(1) AS will_backfill_rows
FROM tmp_node_leave_backfill_final;

SELECT
    f.ticket_id,
    f.id AS node_duration_id,
    f.node_name,
    f.arrive_at,
    f.leave_at_fill,
    TIMESTAMPDIFF(SECOND, f.arrive_at, f.leave_at_fill) AS total_duration_sec_preview
FROM tmp_node_leave_backfill_final f
ORDER BY f.ticket_id, f.id
LIMIT 200;

-- ------------------------------------------------------------
-- Step 3: 执行回填
-- 补齐字段：
--  - leave_at
--  - start_process_at（若为空：优先 first_read_at，否则回退 leave_at）
--  - wait_duration_sec / process_duration_sec / total_duration_sec
-- ------------------------------------------------------------
UPDATE ticket_node_duration n
JOIN tmp_node_leave_backfill_final f ON f.id = n.id
SET
    n.leave_at = f.leave_at_fill,
    n.start_process_at = CASE
        WHEN n.start_process_at IS NOT NULL THEN n.start_process_at
        WHEN n.first_read_at IS NOT NULL AND n.first_read_at <= f.leave_at_fill THEN n.first_read_at
        ELSE f.leave_at_fill
    END,
    n.wait_duration_sec = CASE
        WHEN n.arrive_at IS NOT NULL AND n.first_read_at IS NOT NULL
            THEN GREATEST(TIMESTAMPDIFF(SECOND, n.arrive_at, n.first_read_at), 0)
        ELSE n.wait_duration_sec
    END,
    n.total_duration_sec = CASE
        WHEN n.arrive_at IS NOT NULL
            THEN GREATEST(TIMESTAMPDIFF(SECOND, n.arrive_at, f.leave_at_fill), 0)
        ELSE n.total_duration_sec
    END,
    n.process_duration_sec = GREATEST(
        TIMESTAMPDIFF(
            SECOND,
            CASE
                WHEN n.start_process_at IS NOT NULL THEN n.start_process_at
                WHEN n.first_read_at IS NOT NULL AND n.first_read_at <= f.leave_at_fill THEN n.first_read_at
                ELSE f.leave_at_fill
            END,
            f.leave_at_fill
        ),
        0
    ),
    n.update_by = 'backfill_leave_at_20260511_1',
    n.update_time = NOW()
WHERE n.deleted = 0
  AND n.leave_at IS NULL;

SELECT ROW_COUNT() AS affected_rows;

-- ------------------------------------------------------------
-- Step 4: 回填后校验
-- ------------------------------------------------------------
-- 4.1 仍然 leave_at 为空但已经存在“下一节点到达时间”的记录（理论应尽量接近 0）
SELECT
    COUNT(1) AS remain_open_with_next_node
FROM ticket_node_duration n
WHERE n.deleted = 0
  AND n.leave_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM ticket_node_duration n2
      WHERE n2.deleted = 0
        AND n2.ticket_id = n.ticket_id
        AND n2.arrive_at IS NOT NULL
        AND (
              n.arrive_at IS NULL
              OR n2.arrive_at > n.arrive_at
              OR (n2.arrive_at = n.arrive_at AND n2.id > n.id)
            )
  );

-- 4.2 已关闭节点但缺少开始处理时间（理论应尽量接近 0）
SELECT
    COUNT(1) AS closed_node_without_start_process
FROM ticket_node_duration n
WHERE n.deleted = 0
  AND n.leave_at IS NOT NULL
  AND n.start_process_at IS NULL;

COMMIT;

-- 如需回滚，将上面的 COMMIT 改为 ROLLBACK 后再执行。

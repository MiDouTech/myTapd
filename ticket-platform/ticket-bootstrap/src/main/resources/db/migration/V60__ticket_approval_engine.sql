-- ============================================================
-- V60__ticket_approval_engine.sql
-- 工单审批引擎：引入嵌入式审批任务层（借鉴米多星球审批引擎设计）
-- 兼容现有 FSM 工作流，不破坏任何已有状态机逻辑
-- ============================================================

-- -----------------------------------------------------------
-- 审批任务表（对应米多星球 wf_flow_task，去掉表单相关字段）
-- 每条记录代表某工单某审批节点某审批人的一次审批任务
-- -----------------------------------------------------------
CREATE TABLE `ticket_approval_task` (
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`     bigint(20)   NOT NULL COMMENT '关联工单ID',
    `transition_id` varchar(50)  NOT NULL COMMENT '触发审批的工作流 transition.id',
    `node_key`      varchar(100) NOT NULL COMMENT '审批节点标识（同一 transition 多节点区分）',
    `node_name`     varchar(100) NOT NULL DEFAULT '' COMMENT '审批节点名称',
    `approve_mode`  varchar(20)  NOT NULL DEFAULT 'single' COMMENT '审批模式：single/countersign/orsign/sequential',
    `assignee_id`   bigint(20)   NOT NULL COMMENT '审批人用户ID',
    `assignee_name` varchar(100) NOT NULL DEFAULT '' COMMENT '审批人姓名',
    `task_status`   varchar(20)  NOT NULL DEFAULT 'pending' COMMENT '任务状态：pending/waiting/approved/rejected/transferred/skipped',
    `sort_order`    int(11)      NOT NULL DEFAULT 0 COMMENT '顺序审批时的排序（sequential 模式：0=当前激活，>0=等待中）',
    `due_time`      datetime     DEFAULT NULL COMMENT '审批截止时间（可选，超时预警用）',
    `remark`        varchar(500) DEFAULT NULL COMMENT '审批意见',
    `operate_time`  datetime     DEFAULT NULL COMMENT '审批操作完成时间',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`       tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_assignee_status` (`assignee_id`, `task_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单审批任务表';

-- -----------------------------------------------------------
-- 审批操作记录表（对应米多星球 wf_flow_approval_record）
-- 每次操作写一条，完整追溯审批轨迹
-- -----------------------------------------------------------
CREATE TABLE `ticket_approval_record` (
    `id`                   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`            bigint(20)   NOT NULL COMMENT '关联工单ID',
    `task_id`              bigint(20)   NOT NULL COMMENT '关联审批任务ID',
    `node_key`             varchar(100) NOT NULL COMMENT '审批节点标识',
    `action_type`          varchar(20)  NOT NULL COMMENT '操作类型：approve/reject/transfer',
    `operator_id`          bigint(20)   NOT NULL COMMENT '操作人用户ID',
    `operator_name`        varchar(100) NOT NULL DEFAULT '' COMMENT '操作人姓名',
    `remark`               varchar(500) DEFAULT NULL COMMENT '审批意见',
    `target_assignee_id`   bigint(20)   DEFAULT NULL COMMENT '转交目标人ID',
    `target_assignee_name` varchar(100) DEFAULT NULL COMMENT '转交目标人姓名',
    `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`            varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`            varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`              tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单审批操作记录表';

-- -----------------------------------------------------------
-- 更新审批工单工作流（workflow_id=2）的 transitions，
-- 为「提交审批」流转（submitted → dept_approval）加入 approvalConfig，
-- 实现进入 dept_approval 状态后自动创建审批任务。
-- approvalConfig 说明：
--   passedStatus  - 审批通过后自动流转到的目标状态
--   rejectedStatus - 审批驳回后自动流转到的目标状态
--   nodes - 审批节点列表（此处配置一个「部门负责人审批」示例节点）
-- -----------------------------------------------------------
UPDATE `workflow`
SET `transitions` = JSON_SET(
    `transitions`,
    '$[0]',
    JSON_OBJECT(
        'id', 't01',
        'from', 'submitted',
        'to', 'dept_approval',
        'name', '提交审批',
        'allowedRoles', JSON_ARRAY('SUBMITTER'),
        'requireRemark', false,
        'allowTransfer', false,
        'isReturn', false,
        'requireApproval', true,
        'approvalConfig', JSON_OBJECT(
            'passedStatus', 'executing',
            'rejectedStatus', 'rejected',
            'nodes', JSON_ARRAY(
                JSON_OBJECT(
                    'nodeKey', 'node_01',
                    'nodeName', '部门负责人审批',
                    'approveMode', 'single',
                    'assigneeType', 'groupLeader',
                    'assigneeIds', JSON_ARRAY(),
                    'dueHours', 48
                )
            )
        )
    )
)
WHERE `id` = 2 AND `deleted` = 0;

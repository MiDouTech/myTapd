-- ============================================================
-- V22__init_sys_operation_log.sql
-- 系统操作日志表初始化（工单日志模块）
-- PRD：miduo-md/business/工单日志模块PRD.md
-- API编号段：API000600–API000699
-- ============================================================

CREATE TABLE `sys_operation_log` (
  `id`               bigint(20)   NOT NULL AUTO_INCREMENT                    COMMENT '主键ID',
  `account_id`       bigint(20)   NOT NULL DEFAULT 0                         COMMENT '操作账号ID（0表示系统自动操作）',
  `operator_name`    varchar(50)  NOT NULL DEFAULT ''                        COMMENT '操作人姓名',
  `operator_ip`      varchar(50)  NOT NULL DEFAULT ''                        COMMENT '操作人IP地址',
  `user_agent`       varchar(512) NOT NULL DEFAULT ''                        COMMENT '客户端User-Agent',
  `log_level`        varchar(20)  NOT NULL DEFAULT 'BUSINESS'                COMMENT '日志级别：SYSTEM/BUSINESS/SECURITY/ERROR',
  `app_code`         varchar(50)  NOT NULL DEFAULT ''                        COMMENT '所属应用编码',
  `app_name`         varchar(100) NOT NULL DEFAULT ''                        COMMENT '所属应用名称',
  `module_name`      varchar(100) NOT NULL DEFAULT ''                        COMMENT '操作模块名称',
  `request_path`     varchar(255) NOT NULL DEFAULT ''                        COMMENT '接口请求路径',
  `request_method`   varchar(10)  NOT NULL DEFAULT ''                        COMMENT 'HTTP请求方式',
  `operation_item`   varchar(200) NOT NULL DEFAULT ''                        COMMENT '操作项名称',
  `request_params`   text                                                    COMMENT '请求参数（JSON格式，敏感字段已脱敏）',
  `change_records`   text                                                    COMMENT '变更记录（JSON数组，含fieldName/beforeValue/afterValue）',
  `execute_result`   varchar(20)  NOT NULL DEFAULT 'SUCCESS'                 COMMENT '执行结果：SUCCESS/FAILURE',
  `cost_millis`      int(11)      NOT NULL DEFAULT 0                         COMMENT '接口耗时（毫秒）',
  `error_code`       varchar(50)  DEFAULT NULL                               COMMENT '错误码',
  `error_message`    varchar(512) DEFAULT NULL                               COMMENT '错误信息',
  `error_stack`      text                                                    COMMENT '异常堆栈摘要（前500字符）',
  `operate_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP         COMMENT '操作时间',
  `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP         COMMENT '创建时间',
  `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`        varchar(50)  NOT NULL DEFAULT ''                        COMMENT '创建人',
  `update_by`        varchar(50)  NOT NULL DEFAULT ''                        COMMENT '更新人',
  `deleted`          tinyint(4)   NOT NULL DEFAULT '0'                       COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_account_id`     (`account_id`),
  KEY `idx_operate_time`   (`operate_time`),
  KEY `idx_log_level`      (`log_level`),
  KEY `idx_app_code`       (`app_code`),
  KEY `idx_execute_result` (`execute_result`),
  KEY `idx_deleted`        (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

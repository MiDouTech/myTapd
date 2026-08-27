CREATE TABLE `ticket_saved_query` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        bigint(20)   NOT NULL COMMENT '所属用户ID，关联 sys_user.id',
    `query_name`     varchar(30)  NOT NULL COMMENT '常用查询名称',
    `query_config`   json         NOT NULL COMMENT '查询条件配置',
    `export_fields`  json         NOT NULL COMMENT '导出字段Key及顺序',
    `schema_version` int(11)      NOT NULL DEFAULT 1 COMMENT '配置结构版本',
    `sort_order`     int(11)      NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_saved_query_user_sort` (`user_id`, `deleted`, `sort_order`, `id`),
    KEY `idx_saved_query_user_name` (`user_id`, `query_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人常用工单查询';

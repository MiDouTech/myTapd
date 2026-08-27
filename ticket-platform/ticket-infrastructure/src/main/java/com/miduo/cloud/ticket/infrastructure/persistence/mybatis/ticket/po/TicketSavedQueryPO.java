package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_saved_query")
public class TicketSavedQueryPO extends BaseEntity {
    @TableField("user_id")
    private Long userId;
    @TableField("query_name")
    private String queryName;
    @TableField("query_config")
    private String queryConfig;
    @TableField("export_fields")
    private String exportFields;
    @TableField("schema_version")
    private Integer schemaVersion;
    @TableField("sort_order")
    private Integer sortOrder;
}

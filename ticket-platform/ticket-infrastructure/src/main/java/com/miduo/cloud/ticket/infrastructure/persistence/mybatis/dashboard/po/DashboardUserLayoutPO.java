package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仪表盘个人布局配置PO（数据库映射对象）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dashboard_user_layout")
public class DashboardUserLayoutPO extends BaseEntity {

    /**
     * 用户ID，关联 sys_user.id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 行组Key（overview/trend_category/efficiency_workload）
     */
    @TableField("row_group_key")
    private String rowGroupKey;

    /**
     * 排列序号，越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否固定不可拖拽（1固定/0可拖）
     */
    @TableField("is_fixed")
    private Integer isFixed;
}

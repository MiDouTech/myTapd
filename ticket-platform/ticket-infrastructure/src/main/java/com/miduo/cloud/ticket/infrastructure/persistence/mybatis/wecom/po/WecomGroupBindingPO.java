package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微群绑定配置持久化对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_group_binding")
public class WecomGroupBindingPO extends BaseEntity {

    /**
     * 企微群ChatID
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 群名称
     */
    @TableField("chat_name")
    private String chatName;

    /**
     * 默认工单分类ID
     */
    @TableField("default_category_id")
    private Long defaultCategoryId;

    /**
     * 群Webhook推送地址
     */
    @TableField("webhook_url")
    private String webhookUrl;

    /**
     * 启用状态（0:禁用 1:启用）
     */
    @TableField("is_active")
    private Integer isActive;
}

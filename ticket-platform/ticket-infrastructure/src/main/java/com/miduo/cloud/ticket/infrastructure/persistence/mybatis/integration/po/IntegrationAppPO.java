package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件接入应用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_app")
public class IntegrationAppPO extends BaseEntity {

    @TableField("app_name")
    private String appName;

    @TableField("app_key")
    private String appKey;

    @TableField("app_secret")
    private String appSecret;

    @TableField("system_code")
    private String systemCode;

    @TableField("default_category_id")
    private Long defaultCategoryId;

    @TableField("category_mapping")
    private String categoryMapping;

    @TableField("callback_url")
    private String callbackUrl;

    @TableField("callback_secret")
    private String callbackSecret;

    @TableField("allowed_origins")
    private String allowedOrigins;

    @TableField("permissions")
    private String permissions;

    @TableField("status")
    private Integer status;
}

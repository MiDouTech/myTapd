package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.plugin.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件外部用户与本地账号映射
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plugin_external_user")
public class PluginExternalUserPO extends BaseEntity {

    @TableField("system_code")
    private String systemCode;

    @TableField("external_user_id")
    private String externalUserId;

    @TableField("user_id")
    private Long userId;
}

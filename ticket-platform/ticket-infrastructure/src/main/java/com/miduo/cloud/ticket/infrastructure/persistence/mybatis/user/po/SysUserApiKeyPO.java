package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户个人 API 密钥
 */
@Data
@TableName("sys_user_api_key")
public class SysUserApiKeyPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("name")
    private String name;

    @TableField("key_prefix")
    private String keyPrefix;

    @TableField("secret_hash")
    private String secretHash;

    @TableField("scopes")
    private String scopes;

    /**
     * 1 启用 0 禁用
     */
    @TableField("status")
    private Integer status;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * 累计鉴权成功次数（由 touchLastUsed 异步自增）
     */
    @TableField("invocation_count")
    private Long invocationCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_by")
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}

package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置持久化对象
 */
@Data
@TableName("system_config")
public class SystemConfigPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("config_group")
    private String configGroup;

    @TableField("description")
    private String description;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}

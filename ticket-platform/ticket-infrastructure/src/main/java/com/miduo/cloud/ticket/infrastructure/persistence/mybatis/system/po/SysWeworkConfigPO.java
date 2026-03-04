package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企业微信配置持久化对象
 */
@Data
@TableName("sys_wework_config")
public class SysWeworkConfigPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("corp_id")
    private String corpId;

    @TableField("agent_id")
    private String agentId;

    @TableField("corp_secret")
    private String corpSecret;

    @TableField("api_base_url")
    private String apiBaseUrl;

    @TableField("connect_timeout_ms")
    private Integer connectTimeoutMs;

    @TableField("read_timeout_ms")
    private Integer readTimeoutMs;

    @TableField("schedule_enabled")
    private Integer scheduleEnabled;

    @TableField("schedule_cron")
    private String scheduleCron;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("batch_size")
    private Integer batchSize;

    @TableField("status")
    private Integer status;

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

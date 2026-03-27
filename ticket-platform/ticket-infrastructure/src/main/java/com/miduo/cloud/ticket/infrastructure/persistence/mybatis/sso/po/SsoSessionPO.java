package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sso.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 米多 SSO 会话 PO
 */
@Data
@TableName("sso_session")
public class SsoSessionPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_token")
    private String sessionToken;

    @TableField("miduo_user_id")
    private String miduoUserId;

    @TableField("miduo_user_name")
    private String miduoUserName;

    @TableField("miduo_mobile")
    private String miduoMobile;

    @TableField("miduo_email")
    private String miduoEmail;

    @TableField("miduo_employee_no")
    private String miduoEmployeeNo;

    @TableField("expire_time")
    private Date expireTime;

    @TableField("revoked")
    private Integer revoked;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}

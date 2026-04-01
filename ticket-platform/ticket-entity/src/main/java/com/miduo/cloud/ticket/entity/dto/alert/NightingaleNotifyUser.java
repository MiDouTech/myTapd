package com.miduo.cloud.ticket.entity.dto.alert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 夜莺告警事件中的通知用户对象
 * 对应 notify_users_obj 数组元素
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NightingaleNotifyUser implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private String portrait;

    private String[] roles;

    private Map<String, String> contacts;

    private Integer maintainer;

    @JsonProperty("create_at")
    private Long createAt;

    @JsonProperty("create_by")
    private String createBy;

    @JsonProperty("update_at")
    private Long updateAt;

    @JsonProperty("update_by")
    private String updateBy;

    private Boolean admin;
}

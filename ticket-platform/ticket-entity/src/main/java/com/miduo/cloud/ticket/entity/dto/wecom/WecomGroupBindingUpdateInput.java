package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 企微群绑定更新请求
 */
@Data
public class WecomGroupBindingUpdateInput implements Serializable {

    @Size(max = 200, message = "群名称长度不能超过200字符")
    private String chatName;

    private Long defaultCategoryId;

    @Size(max = 500, message = "Webhook地址长度不能超过500字符")
    private String webhookUrl;

    private Integer isActive;
}

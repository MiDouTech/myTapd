package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 企微群绑定创建请求
 */
@Data
public class WecomGroupBindingCreateInput implements Serializable {

    @NotBlank(message = "群ChatID不能为空")
    @Size(max = 100, message = "群ChatID长度不能超过100字符")
    private String chatId;

    @Size(max = 200, message = "群名称长度不能超过200字符")
    private String chatName;

    private Long defaultCategoryId;

    @Size(max = 500, message = "Webhook地址长度不能超过500字符")
    private String webhookUrl;

    private Integer isActive;
}

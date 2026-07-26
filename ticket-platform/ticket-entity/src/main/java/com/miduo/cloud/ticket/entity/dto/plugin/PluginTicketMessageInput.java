package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 插件用户补充工单信息。
 */
@Data
public class PluginTicketMessageInput implements Serializable {

    @NotBlank(message = "补充内容不能为空")
    @Size(max = 4000, message = "补充内容不能超过4000个字符")
    private String content;

    @Size(max = 9, message = "最多上传9张图片")
    private List<String> attachments;
}

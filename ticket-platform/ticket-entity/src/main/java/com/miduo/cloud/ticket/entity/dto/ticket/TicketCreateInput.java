package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class TicketCreateInput implements Serializable {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 300, message = "工单标题长度不能超过300个字符")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    private String description;

    @NotNull(message = "工单分类不能为空")
    private Long categoryId;

    @NotBlank(message = "优先级不能为空")
    private String priority;

    private Date expectedTime;

    private Long assigneeId;

    private String source;

    private String sourceChatId;

    private Map<String, String> customFields;
}

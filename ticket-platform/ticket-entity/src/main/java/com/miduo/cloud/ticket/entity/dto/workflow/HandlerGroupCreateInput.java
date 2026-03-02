package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 处理组创建请求
 */
@Data
public class HandlerGroupCreateInput implements Serializable {

    @NotBlank(message = "处理组名称不能为空")
    private String name;

    private String description;

    private String skillTags;

    private Long leaderId;

    @NotEmpty(message = "处理组成员不能为空")
    private List<Long> memberIds;
}

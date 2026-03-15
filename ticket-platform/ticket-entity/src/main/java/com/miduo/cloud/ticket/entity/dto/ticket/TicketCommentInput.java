package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 新增工单评论输入
 * 接口编号：API000508
 */
@Data
public class TicketCommentInput implements Serializable {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 20000, message = "评论内容不能超过20000个字符")
    private String content;
}

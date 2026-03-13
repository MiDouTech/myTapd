package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 企微消息自然语言解析入参
 * 接口编号：API000504
 * 产品文档功能：客服信息Tab - 企微消息一键解析赋值
 */
@Data
public class WecomMessageParseInput implements Serializable {

    /**
     * 企微接收到的原始消息文本
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不能超过5000个字符")
    private String message;
}

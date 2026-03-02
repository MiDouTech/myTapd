package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企微群绑定列表输出
 */
@Data
public class WecomGroupBindingListOutput implements Serializable {

    private Long id;

    private String chatId;

    private String chatName;

    private Long defaultCategoryId;

    private String defaultCategoryName;

    private String webhookUrl;

    private Integer isActive;

    private Date createTime;

    private Date updateTime;
}

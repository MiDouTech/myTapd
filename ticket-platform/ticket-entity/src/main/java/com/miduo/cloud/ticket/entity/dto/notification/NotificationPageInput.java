package com.miduo.cloud.ticket.entity.dto.notification;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 通知分页查询输入DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPageInput extends PageInput implements Serializable {

    private String type;

    private Integer isRead;

    private String createTimeStart;

    private String createTimeEnd;
}

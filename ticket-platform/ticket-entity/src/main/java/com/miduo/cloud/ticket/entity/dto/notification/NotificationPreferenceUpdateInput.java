package com.miduo.cloud.ticket.entity.dto.notification;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 通知偏好批量更新输入DTO
 */
@Data
public class NotificationPreferenceUpdateInput implements Serializable {

    @NotEmpty(message = "偏好列表不能为空")
    @Valid
    private List<PreferenceItem> items;

    @Data
    public static class PreferenceItem implements Serializable {

        private String eventType;

        private Integer siteEnabled;

        private Integer wecomEnabled;

        private Integer emailEnabled;
    }
}

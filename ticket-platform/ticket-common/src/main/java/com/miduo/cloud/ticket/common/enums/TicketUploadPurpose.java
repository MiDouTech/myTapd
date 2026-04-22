package com.miduo.cloud.ticket.common.enums;

import org.springframework.util.StringUtils;

/**
 * 工单文件上传场景：问题截图仅允许图片；附件区允许常见办公与媒体格式。
 */
public enum TicketUploadPurpose {

    /**
     * 问题截图等场景，仅允许图片
     */
    SCREENSHOT("screenshot"),

    /**
     * 工单附件区，允许图片、Excel、文本、视频等
     */
    ATTACHMENT("attachment");

    private final String code;

    TicketUploadPurpose(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 解析请求参数；未传或空串时按「截图」处理，与历史接口行为一致。
     */
    public static TicketUploadPurpose fromRequestParam(String param) {
        if (!StringUtils.hasText(param)) {
            return SCREENSHOT;
        }
        String trimmed = param.trim();
        for (TicketUploadPurpose purpose : values()) {
            if (purpose.code.equalsIgnoreCase(trimmed)) {
                return purpose;
            }
        }
        return SCREENSHOT;
    }
}

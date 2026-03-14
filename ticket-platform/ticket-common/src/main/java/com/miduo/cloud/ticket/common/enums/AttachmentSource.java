package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单附件来源枚举
 */
@Getter
@AllArgsConstructor
public enum AttachmentSource {

    WEB("WEB", "Web上传"),
    WECOM_BOT("WECOM_BOT", "企微");

    private final String code;
    private final String label;

    public static AttachmentSource fromCode(String code) {
        if (code == null) {
            return WEB;
        }
        for (AttachmentSource source : values()) {
            if (source.code.equalsIgnoreCase(code)) {
                return source;
            }
        }
        return WEB;
    }
}

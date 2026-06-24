package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 插件开放接口权限
 */
@Getter
@AllArgsConstructor
public enum PluginPermission {

    LAUNCH_TOKEN("plugin:launch-token", "签发LaunchToken"),
    TICKET_CREATE("plugin:ticket:create", "插件建单"),
    TICKET_READ_MINE("plugin:ticket:read-mine", "查询我的工单"),
    ATTACHMENT_UPLOAD("plugin:attachment:upload", "附件上传"),
    OPEN_TICKET_EXPORT("open:ticket:export", "工单导出");

    private final String code;
    private final String label;

    public static Set<String> parsePermissionSet(String permissions) {
        if (permissions == null || permissions.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : permissions.split(",")) {
            if (item != null && !item.trim().isEmpty()) {
                result.add(item.trim());
            }
        }
        return result;
    }

    public static boolean hasPermission(Set<String> permissionSet, PluginPermission permission) {
        if (permissionSet == null || permissionSet.isEmpty() || permission == null) {
            return false;
        }
        return permissionSet.contains(permission.getCode());
    }

    public static String joinDefaultPermissions() {
        return String.join(",",
                LAUNCH_TOKEN.getCode(),
                TICKET_CREATE.getCode(),
                TICKET_READ_MINE.getCode());
    }

    public static boolean isValidPermission(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code.trim()));
    }
}

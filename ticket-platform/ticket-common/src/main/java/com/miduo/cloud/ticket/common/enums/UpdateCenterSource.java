package com.miduo.cloud.ticket.common.enums;

/**
 * 更新中心数据来源
 */
public enum UpdateCenterSource {

    LOCAL("local"),
    GITHUB("github"),
    NONE("none");

    private final String code;

    UpdateCenterSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

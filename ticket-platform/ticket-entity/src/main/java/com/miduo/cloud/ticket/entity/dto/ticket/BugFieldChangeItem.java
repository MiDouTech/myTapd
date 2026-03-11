package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 单个字段变更明细
 * 存储在 ticket_log.remark JSON 的 fields 数组中
 */
@Data
public class BugFieldChangeItem implements Serializable {

    /**
     * 字段标识（英文，用于前端筛选）
     */
    private String fieldName;

    /**
     * 字段中文名（用于前端展示，与右侧信息面板字段名保持一致）
     */
    private String fieldLabel;

    /**
     * 旧值（原始存储值）
     */
    private String oldValue;

    /**
     * 旧值展示文本（枚举字段显示中文 label，其余与 oldValue 相同）
     */
    private String oldLabel;

    /**
     * 新值（原始存储值）
     */
    private String newValue;

    /**
     * 新值展示文本（枚举字段显示中文 label，其余与 newValue 相同）
     */
    private String newLabel;
}

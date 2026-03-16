package com.miduo.cloud.ticket.entity.dto.operationlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字段变更记录项
 * PRD §3.2.3 操作内容区（变更记录）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRecordItem implements Serializable {

    /** 字段名 */
    private String fieldName;

    /** 变更前的值 */
    private String beforeValue;

    /** 变更后的值 */
    private String afterValue;
}

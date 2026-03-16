package com.miduo.cloud.ticket.entity.dto.operationlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 所属应用枚举输出DTO
 * 接口编号：API000605
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppCodeOutput implements Serializable {

    /** 应用编码枚举值 */
    private String code;

    /** 应用名称 */
    private String appName;
}

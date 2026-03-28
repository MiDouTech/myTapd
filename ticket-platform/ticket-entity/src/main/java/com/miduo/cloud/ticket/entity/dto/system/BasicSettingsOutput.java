package com.miduo.cloud.ticket.entity.dto.system;

import lombok.Data;

import java.io.Serializable;

/**
 * 基础参数配置输出对象
 */
@Data
public class BasicSettingsOutput implements Serializable {

    private String systemName;

    private String timezone;

    private String workTimeStart;

    private String workTimeEnd;

    private Integer defaultPageSize;
}

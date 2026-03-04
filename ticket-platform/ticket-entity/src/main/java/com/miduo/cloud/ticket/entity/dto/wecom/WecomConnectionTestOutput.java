package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;

/**
 * 企业微信连接测试结果
 */
@Data
public class WecomConnectionTestOutput implements Serializable {

    private Boolean success;
    private String message;
    private Integer departmentCount;
}

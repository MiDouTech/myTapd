package com.miduo.cloud.ticket.entity.dto.integration;

import lombok.Data;

import java.io.Serializable;

/**
 * 轮换 AppSecret 输出
 * 接口编号：API000530
 */
@Data
public class IntegrationAppRotateSecretOutput implements Serializable {

    private String appKey;

    private String appSecret;
}

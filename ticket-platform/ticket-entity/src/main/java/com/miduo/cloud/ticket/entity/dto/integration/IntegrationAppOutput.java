package com.miduo.cloud.ticket.entity.dto.integration;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 接入应用输出
 */
@Data
public class IntegrationAppOutput implements Serializable {

    private Long id;

    private String appName;

    private String appKey;

    private String appSecret;

    private String systemCode;

    private Long defaultCategoryId;

    private Map<String, Long> categoryMapping;

    private String callbackUrl;

    private String allowedOrigins;

    private String permissions;

    private Integer status;

    private Date createTime;

    private Date updateTime;
}

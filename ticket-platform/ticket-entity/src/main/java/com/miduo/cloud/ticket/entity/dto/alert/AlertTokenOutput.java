package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警接入Token - 输出
 */
@Data
public class AlertTokenOutput implements Serializable {

    private String token;

    private String webhookUrl;
}

package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import java.io.Serializable;

/**
 * LaunchToken 输出
 */
@Data
public class PluginLaunchTokenOutput implements Serializable {

    private String launchToken;

    private Long expireSeconds;
}

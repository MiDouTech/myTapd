package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 签发 LaunchToken 请求
 * 接口编号：API000531
 */
@Data
public class PluginLaunchTokenInput implements Serializable {

    @NotBlank(message = "外部用户ID不能为空")
    private String externalUserId;

    @NotBlank(message = "用户姓名不能为空")
    private String userName;

    private String dept;

    private String role;

    private String mobile;
}

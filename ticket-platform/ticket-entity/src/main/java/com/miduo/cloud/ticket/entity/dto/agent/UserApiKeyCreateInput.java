package com.miduo.cloud.ticket.entity.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建个人 API 密钥
 */
@Data
@Schema(description = "创建个人API密钥请求")
public class UserApiKeyCreateInput {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100")
    @Schema(description = "密钥显示名称，如「公司笔记本」")
    private String name;
}

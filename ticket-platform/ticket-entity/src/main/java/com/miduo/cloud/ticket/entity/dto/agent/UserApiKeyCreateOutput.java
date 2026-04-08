package com.miduo.cloud.ticket.entity.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建密钥响应（仅本次返回完整 apiKey）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建个人API密钥响应")
public class UserApiKeyCreateOutput {

    @Schema(description = "记录ID")
    private Long id;

    /**
     * 完整密钥，格式 mdt_{uuid}_{secret}，仅创建时返回一次
     */
    @Schema(description = "完整API密钥（仅创建时返回一次，请立即保存）")
    private String apiKey;
}

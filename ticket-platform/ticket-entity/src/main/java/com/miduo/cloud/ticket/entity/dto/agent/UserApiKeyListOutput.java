package com.miduo.cloud.ticket.entity.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 密钥列表项（脱敏）
 */
@Data
@Schema(description = "个人API密钥列表项")
public class UserApiKeyListOutput {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "显示名称")
    private String name;

    /**
     * 用于展示的密钥前缀片段（非完整密钥）
     */
    @Schema(description = "密钥前缀（脱敏）")
    private String keyPrefixDisplay;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "最后使用时间")
    private LocalDateTime lastUsedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

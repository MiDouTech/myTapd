package com.miduo.cloud.ticket.controller.agent;

import com.miduo.cloud.ticket.application.agent.UserApiKeyApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.agent.UserApiKeyCreateInput;
import com.miduo.cloud.ticket.entity.dto.agent.UserApiKeyCreateOutput;
import com.miduo.cloud.ticket.entity.dto.agent.UserApiKeyListOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 个人 API 密钥管理（须登录 JWT，用于 IDE/龙虾 配置）
 */
@RestController
@RequestMapping("/api/user/api-key")
@Tag(name = "个人API密钥", description = "IDE/Agent 调用工单的密钥管理")
public class UserApiKeyController {

    @Resource
    private UserApiKeyApplicationService userApiKeyApplicationService;

    /**
     * 创建个人 API 密钥
     * 接口编号：API000509
     */
    @PostMapping("/create")
    @Operation(summary = "创建个人API密钥", description = "接口编号：API000509；完整密钥仅本次返回")
    public ApiResult<UserApiKeyCreateOutput> create(@Valid @RequestBody UserApiKeyCreateInput input) {
        UserApiKeyCreateOutput out = userApiKeyApplicationService.createKey(getCurrentUserId(), input);
        return ApiResult.success(out);
    }

    /**
     * 个人 API 密钥列表
     * 接口编号：API000510
     */
    @GetMapping("/list")
    @Operation(summary = "个人API密钥列表", description = "接口编号：API000510；列表项含 invocationCount（X-Api-Key 鉴权成功累计次数，异步更新）")
    public ApiResult<List<UserApiKeyListOutput>> list() {
        List<UserApiKeyListOutput> list = userApiKeyApplicationService.listKeys(getCurrentUserId());
        return ApiResult.success(list);
    }

    /**
     * 禁用密钥
     * 接口编号：API000511
     */
    @PutMapping("/disable/{id}")
    @Operation(summary = "禁用个人API密钥", description = "接口编号：API000511")
    public ApiResult<Void> disable(@PathVariable Long id) {
        userApiKeyApplicationService.disableKey(getCurrentUserId(), id);
        return ApiResult.success();
    }

    /**
     * 删除密钥
     * 接口编号：API000512
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除个人API密钥", description = "接口编号：API000512")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userApiKeyApplicationService.deleteKey(getCurrentUserId(), id);
        return ApiResult.success();
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

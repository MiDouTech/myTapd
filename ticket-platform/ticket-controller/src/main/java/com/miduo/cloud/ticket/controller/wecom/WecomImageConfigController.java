package com.miduo.cloud.ticket.controller.wecom;

import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomImageConfigOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomImageConfigUpdateInput;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 企微图片处理配置管理接口（P2 预留）
 * Task024：企微图片消息工单关联
 */
@Tag(name = "企微图片配置", description = "企微图片消息处理参数配置管理（P2 预留接口）")
@RestController
@RequestMapping("/api/wecom/image-config")
public class WecomImageConfigController {

    private final WecomProperties wecomProperties;

    public WecomImageConfigController(WecomProperties wecomProperties) {
        this.wecomProperties = wecomProperties;
    }

    /**
     * 查询企微图片处理配置
     * 接口编号：API000437
     * 产品文档功能：Task024 企微图片配置查询（P2预留）
     */
    @Operation(summary = "查询企微图片处理配置", description = "接口编号：API000437")
    @GetMapping("/detail")
    public ApiResult<WecomImageConfigOutput> detail() {
        WecomProperties.ImageConfig config = wecomProperties.getImage();
        WecomImageConfigOutput output = new WecomImageConfigOutput();
        if (config != null) {
            output.setEnabled(config.isEnabled());
            output.setAssociationWindowMinutes(config.getAssociationWindowMinutes());
            output.setTimeoutStrategy(config.getTimeoutStrategy());
            output.setNotifyOnPending(config.isNotifyOnPending());
            output.setMaxImagesPerTicket(config.getMaxImagesPerTicket());
        } else {
            output.setEnabled(true);
            output.setAssociationWindowMinutes(5);
            output.setTimeoutStrategy("CREATE_TICKET");
            output.setNotifyOnPending(false);
            output.setMaxImagesPerTicket(10);
        }
        return ApiResult.success(output);
    }

    /**
     * 更新企微图片处理配置（P2 预留，当前写入内存配置）
     * 接口编号：API000438
     * 产品文档功能：Task024 企微图片配置更新（P2预留）
     */
    @Operation(summary = "更新企微图片处理配置", description = "接口编号：API000438")
    @PutMapping("/update")
    public ApiResult<Void> update(@Valid @RequestBody WecomImageConfigUpdateInput input) {
        WecomProperties.ImageConfig config = wecomProperties.getImage();
        if (config == null) {
            config = new WecomProperties.ImageConfig();
            wecomProperties.setImage(config);
        }
        config.setEnabled(input.isEnabled());
        config.setAssociationWindowMinutes(input.getAssociationWindowMinutes());
        config.setTimeoutStrategy(input.getTimeoutStrategy());
        config.setNotifyOnPending(input.isNotifyOnPending());
        config.setMaxImagesPerTicket(input.getMaxImagesPerTicket());
        return ApiResult.success(null);
    }
}

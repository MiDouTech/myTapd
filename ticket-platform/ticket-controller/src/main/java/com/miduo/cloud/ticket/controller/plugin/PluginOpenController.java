package com.miduo.cloud.ticket.controller.plugin;

import com.miduo.cloud.ticket.application.plugin.PluginConfigApplicationService;
import com.miduo.cloud.ticket.application.plugin.PluginLaunchTokenApplicationService;
import com.miduo.cloud.ticket.application.plugin.PluginLaunchTokenClaims;
import com.miduo.cloud.ticket.application.plugin.PluginTicketApplicationService;
import com.miduo.cloud.ticket.common.constants.OpenApiAuthConstants;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.plugin.*;
import com.miduo.cloud.ticket.entity.dto.ticket.ImageUploadOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 插件开放接口
 */
@Tag(name = "工单插件", description = "业务原生工单插件开放接口")
@RestController
@RequestMapping("/api/open/v1/plugin")
public class PluginOpenController {

    private final PluginLaunchTokenApplicationService pluginLaunchTokenApplicationService;
    private final PluginTicketApplicationService pluginTicketApplicationService;
    private final PluginConfigApplicationService pluginConfigApplicationService;

    public PluginOpenController(PluginLaunchTokenApplicationService pluginLaunchTokenApplicationService,
                                PluginTicketApplicationService pluginTicketApplicationService,
                                PluginConfigApplicationService pluginConfigApplicationService) {
        this.pluginLaunchTokenApplicationService = pluginLaunchTokenApplicationService;
        this.pluginTicketApplicationService = pluginTicketApplicationService;
        this.pluginConfigApplicationService = pluginConfigApplicationService;
    }

    /**
     * 签发 LaunchToken
     * 接口编号：API000531
     */
    @Operation(summary = "签发LaunchToken", description = "接口编号：API000531")
    @PostMapping("/launch-token")
    public ApiResult<PluginLaunchTokenOutput> issueLaunchToken(HttpServletRequest request,
                                                                 @Valid @RequestBody PluginLaunchTokenInput input) {
        String appKey = request.getHeader(OpenApiAuthConstants.HEADER_APP_KEY);
        return ApiResult.success(pluginLaunchTokenApplicationService.issueLaunchToken(appKey, input));
    }

    /**
     * 插件创建工单
     * 接口编号：API000532
     */
    @Operation(summary = "插件创建工单", description = "接口编号：API000532")
    @PostMapping("/tickets")
    public ApiResult<PluginTicketCreateOutput> createTicket(@RequestHeader("Authorization") String authorization,
                                                            @Valid @RequestBody PluginTicketCreateInput input) {
        PluginLaunchTokenClaims claims = pluginLaunchTokenApplicationService.requireValidToken(authorization);
        return ApiResult.success(pluginTicketApplicationService.createTicket(claims, input));
    }

    /**
     * 插件富文本图片上传
     * 接口编号：API000535
     */
    @Operation(summary = "插件富文本图片上传", description = "接口编号：API000535")
    @PostMapping(value = "/attachments/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<ImageUploadOutput> uploadAttachmentImage(@RequestHeader("Authorization") String authorization,
                                                              @RequestParam("file") MultipartFile file) {
        PluginLaunchTokenClaims claims = pluginLaunchTokenApplicationService.requireValidToken(authorization);
        return ApiResult.success(pluginTicketApplicationService.uploadImage(claims, file));
    }

    /**
     * 插件我的工单列表
     * 接口编号：API000533
     */
    @Operation(summary = "插件我的工单列表", description = "接口编号：API000533")
    @GetMapping("/tickets/mine")
    public ApiResult<PageOutput<PluginTicketSummaryOutput>> listMineTickets(
            @RequestHeader("Authorization") String authorization,
            @Valid PluginTicketMinePageInput input) {
        PluginLaunchTokenClaims claims = pluginLaunchTokenApplicationService.requireValidToken(authorization);
        return ApiResult.success(pluginTicketApplicationService.listMineTickets(claims, input));
    }

    /**
     * 插件工单详情摘要
     * 接口编号：API000534
     */
    @Operation(summary = "插件工单详情摘要", description = "接口编号：API000534")
    @GetMapping("/tickets/{ticketNo}")
    public ApiResult<PluginTicketSummaryOutput> getTicketSummary(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String ticketNo) {
        PluginLaunchTokenClaims claims = pluginLaunchTokenApplicationService.requireValidToken(authorization);
        return ApiResult.success(pluginTicketApplicationService.getTicketSummary(claims, ticketNo));
    }

    /**
     * 插件初始化配置
     * 接口编号：API000537
     */
    @Operation(summary = "插件初始化配置", description = "接口编号：API000537")
    @GetMapping("/config")
    public ApiResult<PluginConfigOutput> getConfig(@RequestParam String appKey, HttpServletRequest request) {
        String origin = resolveOrigin(request);
        return ApiResult.success(pluginConfigApplicationService.getConfig(appKey, origin));
    }

    private String resolveOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            return origin.trim();
        }
        String referer = request.getHeader("Referer");
        if (!StringUtils.hasText(referer)) {
            return null;
        }
        try {
            java.net.URL url = new java.net.URL(referer.trim());
            return url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "");
        } catch (Exception ex) {
            return null;
        }
    }
}

package com.miduo.cloud.ticket.controller.notification;

import com.miduo.cloud.ticket.application.notification.NotificationApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.notification.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 通知中心控制器
 */
@Tag(name = "通知中心", description = "站内通知列表、已读标记、偏好管理")
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationApplicationService notificationService;

    public NotificationController(NotificationApplicationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 分页查询通知列表
     * 接口编号：API000004
     * 产品文档功能：4.8 通知中心 - 站内通知列表
     */
    @Operation(summary = "分页查询通知列表", description = "接口编号：API000004")
    @GetMapping("/page")
    public ApiResult<PageOutput<NotificationOutput>> pageNotifications(NotificationPageInput input) {
        Long currentUserId = getCurrentUserId();
        PageOutput<NotificationOutput> result = notificationService.pageNotifications(currentUserId, input);
        return ApiResult.success(result);
    }

    /**
     * 标记通知为已读
     * 接口编号：API000005
     * 产品文档功能：4.8 通知中心 - 标记通知已读
     */
    @Operation(summary = "标记通知为已读", description = "接口编号：API000005")
    @PutMapping("/read/{id}")
    public ApiResult<Void> markAsRead(@PathVariable("id") Long id) {
        Long currentUserId = getCurrentUserId();
        notificationService.markAsRead(currentUserId, id);
        return ApiResult.success();
    }

    /**
     * 批量标记所有未读通知为已读
     * 接口编号：API000006
     * 产品文档功能：4.8 通知中心 - 全部标记已读
     */
    @Operation(summary = "全部标记已读", description = "接口编号：API000006")
    @PutMapping("/read/all")
    public ApiResult<Void> markAllAsRead() {
        Long currentUserId = getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ApiResult.success();
    }

    /**
     * 查询未读通知数量
     * 接口编号：API000007
     * 产品文档功能：4.8 通知中心 - 未读数量
     */
    @Operation(summary = "查询未读通知数量", description = "接口编号：API000007")
    @GetMapping("/unread/count")
    public ApiResult<NotificationUnreadCountOutput> getUnreadCount() {
        Long currentUserId = getCurrentUserId();
        NotificationUnreadCountOutput result = notificationService.getUnreadCount(currentUserId);
        return ApiResult.success(result);
    }

    /**
     * 获取用户通知偏好
     * 接口编号：API000008
     * 产品文档功能：4.8 通知中心 - 通知偏好设置
     */
    @Operation(summary = "获取用户通知偏好", description = "接口编号：API000008")
    @GetMapping("/preference")
    public ApiResult<List<NotificationPreferenceOutput>> getPreferences() {
        Long currentUserId = getCurrentUserId();
        List<NotificationPreferenceOutput> result = notificationService.getPreferences(currentUserId);
        return ApiResult.success(result);
    }

    /**
     * 更新用户通知偏好
     * 接口编号：API000009
     * 产品文档功能：4.8 通知中心 - 更新通知偏好
     */
    @Operation(summary = "更新用户通知偏好", description = "接口编号：API000009")
    @PutMapping("/preference/update")
    public ApiResult<Void> updatePreferences(@Valid @RequestBody NotificationPreferenceUpdateInput input) {
        Long currentUserId = getCurrentUserId();
        notificationService.updatePreferences(currentUserId, input);
        return ApiResult.success();
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

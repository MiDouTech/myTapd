package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketImageUploadApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketUploadPurpose;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.controller.annotation.OperationLog;
import com.miduo.cloud.ticket.entity.dto.ticket.ImageUploadOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketAttachmentSaveInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 工单图片上传 Controller
 */
@RestController
@RequestMapping("/api/ticket")
@Tag(name = "工单管理", description = "工单核心管理接口")
public class TicketImageController {

    @Resource
    private TicketImageUploadApplicationService imageUploadService;

    /**
     * 上传工单图片
     * 接口编号：API000502
     * 产品文档功能：工单处理 - 上传图片到七牛云并保存附件记录
     *
     * @param ticketId       工单ID
     * @param file           上传文件
     * @param uploadPurpose  可选：screenshot（默认，仅图片）；attachment（附件区：Excel/文本/视频等）
     * @return 文件访问URL及文件信息
     */
    @OperationLog(moduleName = "工单管理", operationItem = "上传工单文件", recordParams = false)
    @PostMapping("/{ticketId}/image/upload")
    @Operation(summary = "上传工单图片", description = "接口编号：API000502；uploadPurpose=attachment 时支持附件格式")
    public ApiResult<ImageUploadOutput> uploadTicketImage(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadPurpose", required = false) String uploadPurpose) {
        Long currentUserId = getCurrentUserId();
        TicketUploadPurpose purpose = TicketUploadPurpose.fromRequestParam(uploadPurpose);
        ImageUploadOutput output = imageUploadService.uploadTicketImage(ticketId, file, currentUserId, purpose);
        return ApiResult.success(output);
    }

    /**
     * 删除工单附件
     * 接口编号：API000503
     * 产品文档功能：工单处理 - 删除已上传的附件
     *
     * @param attachmentId 附件ID
     * @return 操作结果
     */
    @OperationLog(moduleName = "工单管理", operationItem = "删除工单附件", recordParams = false)
    @DeleteMapping("/attachment/delete/{attachmentId}")
    @Operation(summary = "删除工单附件", description = "接口编号：API000503")
    public ApiResult<Void> deleteTicketAttachment(@PathVariable Long attachmentId) {
        Long currentUserId = getCurrentUserId();
        imageUploadService.deleteTicketAttachment(attachmentId, currentUserId);
        return ApiResult.success();
    }

    /**
     * 保存附件记录（前端已上传至七牛云后调用，仅记录URL）
     * 接口编号：API000504
     * 产品文档功能：工单处理 - 保存前端直传后的附件信息
     *
     * @param input 附件保存请求
     * @return 操作结果
     */
    @OperationLog(moduleName = "工单管理", operationItem = "保存工单附件记录")
    @PostMapping("/attachment/save")
    @Operation(summary = "保存工单附件记录", description = "接口编号：API000504")
    public ApiResult<Void> saveTicketAttachment(@Valid @RequestBody TicketAttachmentSaveInput input) {
        Long currentUserId = getCurrentUserId();
        imageUploadService.saveAttachment(input, currentUserId);
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

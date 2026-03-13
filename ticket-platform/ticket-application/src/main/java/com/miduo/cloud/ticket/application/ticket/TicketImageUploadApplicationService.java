package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.ticket.ImageUploadOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketAttachmentSaveInput;
import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketAttachmentMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketAttachmentPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 工单图片上传应用服务
 */
@Service
public class TicketImageUploadApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TicketImageUploadApplicationService.class);

    @Resource
    private QiniuUploadService qiniuUploadService;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private TicketAttachmentMapper attachmentMapper;

    /**
     * 上传图片到七牛云并保存附件记录
     *
     * @param ticketId 工单ID
     * @param file     图片文件
     * @param userId   上传人ID
     * @return 图片上传结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ImageUploadOutput uploadTicketImage(Long ticketId, MultipartFile file, Long userId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        String imageUrl = qiniuUploadService.uploadImage(file);

        TicketAttachmentPO attachment = new TicketAttachmentPO();
        attachment.setTicketId(ticketId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(imageUrl);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setUploadedBy(userId);
        attachmentMapper.insert(attachment);

        log.info("工单图片上传成功: ticketId={}, url={}, uploadedBy={}", ticketId, imageUrl, userId);

        ImageUploadOutput output = new ImageUploadOutput();
        output.setUrl(imageUrl);
        output.setFileName(file.getOriginalFilename());
        output.setFileSize(file.getSize());
        output.setFileType(file.getContentType());
        return output;
    }

    /**
     * 删除工单附件
     *
     * @param attachmentId 附件ID
     * @param userId       操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTicketAttachment(Long attachmentId, Long userId) {
        TicketAttachmentPO attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "附件不存在");
        }

        attachmentMapper.deleteById(attachmentId);
        log.info("工单附件删除成功: attachmentId={}, operatorId={}", attachmentId, userId);
    }

    /**
     * 仅保存附件记录（不上传文件，用于外部已上传后记录URL）
     *
     * @param input  附件保存请求
     * @param userId 上传人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAttachment(TicketAttachmentSaveInput input, Long userId) {
        TicketPO ticket = ticketMapper.selectById(input.getTicketId());
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        TicketAttachmentPO attachment = new TicketAttachmentPO();
        attachment.setTicketId(input.getTicketId());
        attachment.setFileName(input.getFileName());
        attachment.setFilePath(input.getFileUrl());
        attachment.setFileSize(input.getFileSize());
        attachment.setFileType(input.getFileType());
        attachment.setUploadedBy(userId);
        attachmentMapper.insert(attachment);

        log.info("工单附件记录保存成功: ticketId={}, url={}", input.getTicketId(), input.getFileUrl());
    }
}

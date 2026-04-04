package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.enums.AttachmentSource;
import com.miduo.cloud.ticket.common.enums.WecomPendingImageStatus;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuProperties;
import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketAttachmentMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketAttachmentPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomPendingImageMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomPendingImagePO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 企微图片消息处理总入口服务
 * 接口编号：（内部服务）
 * Task024：企微图片消息工单关联
 */
@Service
public class WecomImageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(WecomImageHandlerService.class);

    private static final String MEDIA_TYPE_IMAGE = "image";
    private static final String MEDIA_TYPE_VIDEO = "video";

    private final WecomImageDownloadService imageDownloadService;
    private final WecomPendingImageMapper pendingImageMapper;
    private final TicketAttachmentMapper attachmentMapper;
    private final TicketMapper ticketMapper;
    private final WecomProperties wecomProperties;
    private final StringRedisTemplate redisTemplate;
    private final WecomClient wecomClient;
    private final SysUserMapper sysUserMapper;
    private final QiniuUploadService qiniuUploadService;
    private final QiniuProperties qiniuProperties;

    public WecomImageHandlerService(WecomImageDownloadService imageDownloadService,
                                    WecomPendingImageMapper pendingImageMapper,
                                    TicketAttachmentMapper attachmentMapper,
                                    TicketMapper ticketMapper,
                                    WecomProperties wecomProperties,
                                    StringRedisTemplate redisTemplate,
                                    WecomClient wecomClient,
                                    SysUserMapper sysUserMapper,
                                    QiniuUploadService qiniuUploadService,
                                    QiniuProperties qiniuProperties) {
        this.imageDownloadService = imageDownloadService;
        this.pendingImageMapper = pendingImageMapper;
        this.attachmentMapper = attachmentMapper;
        this.ticketMapper = ticketMapper;
        this.wecomProperties = wecomProperties;
        this.redisTemplate = redisTemplate;
        this.wecomClient = wecomClient;
        this.sysUserMapper = sysUserMapper;
        this.qiniuUploadService = qiniuUploadService;
        this.qiniuProperties = qiniuProperties;
    }

    /**
     * 异步处理企微图片消息：下载 → 上传七牛云 → 暂存 → 尝试关联
     * 通过 @Async 确保不阻塞主回调流程（企微要求 5 秒内响应）
     *
     * @param message 企微回调消息
     */
    @Async
    public void handleImageMessageAsync(WecomCallbackMessageDTO message) {
        if (message == null) {
            return;
        }
        WecomProperties.ImageConfig config = wecomProperties.getImage();
        if (config == null || !config.isEnabled()) {
            log.info("企微图片处理功能已禁用，跳过: msgId={}", message.getMsgId());
            return;
        }

        log.info("开始异步处理企微图片消息: msgId={}, chatId={}, fromUserId={}",
                message.getMsgId(), message.getChatId(), message.getFromWecomUserid());

        String qiniuUrl = imageDownloadService.downloadAndUpload(
                message.getMediaId(), message.getPicUrl(),
                message.getDownloadUrl(), message.getAesKey(),
                message.getMsgId());

        if (qiniuUrl == null) {
            savePendingImageRecord(message, null, WecomPendingImageStatus.FAILED);
            log.warn("企微图片下载/上传失败，已标记FAILED: msgId={}", message.getMsgId());
            return;
        }

        WecomPendingImagePO pending = savePendingImageRecord(message, qiniuUrl, WecomPendingImageStatus.PENDING);
        if (pending == null) {
            return;
        }

        boolean linked = tryLinkToRecentTicket(pending, config);

        if (!linked && config.isNotifyOnPending()) {
            sendPendingNotification(message.getChatId(), message.getFromWecomUserid(), message.getResponseUrl());
        }
    }

    /**
     * 异步处理企微视频消息：流式下载视频 → 上传七牛云 → 下载缩略图 → 暂存 → 尝试关联
     * 通过 @Async 确保不阻塞主回调流程（企微要求 5 秒内响应）
     *
     * @param message 企微回调消息（MsgType=video）
     */
    @Async
    public void handleVideoMessageAsync(WecomCallbackMessageDTO message) {
        if (message == null) {
            return;
        }
        WecomProperties.ImageConfig config = wecomProperties.getImage();
        if (config == null || !config.isEnabled()) {
            log.info("企微媒体处理功能已禁用，跳过视频: msgId={}", message.getMsgId());
            return;
        }

        log.info("开始异步处理企微视频消息: msgId={}, chatId={}, fromUserId={}",
                message.getMsgId(), message.getChatId(), message.getFromWecomUserid());

        String videoUrl = null;
        if (message.getMediaId() != null && !message.getMediaId().isEmpty()) {
            InputStream videoStream = wecomClient.downloadMediaStream(message.getMediaId());
            videoUrl = qiniuUploadService.uploadStream(
                    videoStream,
                    "wecom_" + message.getMsgId() + ".mp4",
                    "video/mp4",
                    qiniuProperties.getVideoPathPrefix());
        }

        if (videoUrl == null) {
            savePendingMediaRecord(message, null, null, MEDIA_TYPE_VIDEO, WecomPendingImageStatus.FAILED);
            log.warn("企微视频下载/上传失败，已标记FAILED: msgId={}", message.getMsgId());
            return;
        }

        String thumbUrl = null;
        if (message.getThumbMediaId() != null && !message.getThumbMediaId().isEmpty()) {
            byte[] thumbBytes = wecomClient.downloadMediaById(message.getThumbMediaId());
            if (thumbBytes != null && thumbBytes.length > 0) {
                thumbUrl = qiniuUploadService.uploadImageBytes(thumbBytes,
                        "thumb_" + message.getMsgId() + ".jpg");
            }
        }

        WecomPendingImagePO pending = savePendingMediaRecord(message, videoUrl, thumbUrl, MEDIA_TYPE_VIDEO,
                WecomPendingImageStatus.PENDING);
        if (pending == null) {
            return;
        }

        boolean linked = tryLinkToRecentTicket(pending, config);

        if (!linked && config.isNotifyOnPending()) {
            sendPendingNotification(message.getChatId(), message.getFromWecomUserid(), message.getResponseUrl());
        }
    }

    /**
     * 规则B：先图后文 — 文字建单成功后，批量关联该用户该群 PENDING 状态的暂存图片
     *
     * @param ticketId    新建工单 ID
     * @param chatId      来源群 ID
     * @param fromUserId  发送人企微 UserId
     * @return 关联的图片数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int linkPendingImagesToTicket(Long ticketId, String chatId, String fromUserId) {
        if (ticketId == null || chatId == null || fromUserId == null) {
            return 0;
        }
        WecomProperties.ImageConfig config = wecomProperties.getImage();
        if (config == null || !config.isEnabled()) {
            return 0;
        }

        int windowMinutes = config.getAssociationWindowMinutes();
        Date windowStart = minutesBefore(new Date(), windowMinutes);
        List<WecomPendingImagePO> pendingList = pendingImageMapper.selectPendingByUserAndChat(
                chatId, fromUserId, windowStart);

        if (pendingList == null || pendingList.isEmpty()) {
            return 0;
        }

        int maxImages = config.getMaxImagesPerTicket();
        int existingCount = countExistingAttachments(ticketId);
        int linked = 0;

        for (WecomPendingImagePO pending : pendingList) {
            if (existingCount + linked >= maxImages) {
                log.warn("工单图片数已达上限 {}，停止关联: ticketId={}", maxImages, ticketId);
                break;
            }
            createAttachmentRecord(ticketId, pending.getQiniuUrl(), pending.getMsgId(),
                    pending.getMediaType() != null ? pending.getMediaType() : MEDIA_TYPE_IMAGE);
            updatePendingStatus(pending.getId(), ticketId, WecomPendingImageStatus.LINKED);
            linked++;
        }

        if (linked > 0) {
            log.info("规则B先图后文关联成功: ticketId={}, chatId={}, fromUserId={}, linked={}",
                    ticketId, chatId, fromUserId, linked);
        }
        return linked;
    }

    /**
     * 规则A：先文后图 — 图片收到时检查是否有近期工单可关联
     * 找到即关联，未找到保持 PENDING 等待后续文字建单
     * 优先按 chatId + creatorId 精确匹配，防止群聊中不同用户的图片互相串联；
     * 当发送人未关联系统账号时降级为仅按 chatId 匹配。
     *
     * @return true 表示已成功关联，false 表示未关联（保持 PENDING）
     */
    private boolean tryLinkToRecentTicket(WecomPendingImagePO pending, WecomProperties.ImageConfig config) {
        int windowMinutes = config.getAssociationWindowMinutes();
        Date windowStart = minutesBefore(new Date(), windowMinutes);

        Long creatorId = resolveCreatorId(pending.getFromUserId());

        LambdaQueryWrapper<TicketPO> wrapper = new LambdaQueryWrapper<TicketPO>()
                .eq(TicketPO::getSourceChatId, pending.getChatId())
                .ge(TicketPO::getCreateTime, windowStart)
                .orderByDesc(TicketPO::getCreateTime)
                .last("LIMIT 1");
        if (creatorId != null) {
            wrapper.eq(TicketPO::getCreatorId, creatorId);
        }
        TicketPO recentTicket = ticketMapper.selectOne(wrapper);

        if (recentTicket == null) {
            log.info("规则A：未找到近期工单，保持PENDING等待: msgId={}, chatId={}", pending.getMsgId(), pending.getChatId());
            return false;
        }

        int maxImages = config.getMaxImagesPerTicket();
        int existingCount = countExistingAttachments(recentTicket.getId());
        if (existingCount >= maxImages) {
            log.warn("工单图片数已达上限 {}，规则A不关联: ticketId={}", maxImages, recentTicket.getId());
            return false;
        }

        createAttachmentRecord(recentTicket.getId(), pending.getQiniuUrl(), pending.getMsgId(),
                pending.getMediaType() != null ? pending.getMediaType() : MEDIA_TYPE_IMAGE);
        updatePendingStatus(pending.getId(), recentTicket.getId(), WecomPendingImageStatus.LINKED);
        log.info("规则A先文后图关联成功: msgId={}, ticketId={}", pending.getMsgId(), recentTicket.getId());
        return true;
    }

    /**
     * 写入 wecom_pending_image 暂存记录（图片，向后兼容原有调用）
     */
    private WecomPendingImagePO savePendingImageRecord(WecomCallbackMessageDTO message,
                                                        String qiniuUrl,
                                                        WecomPendingImageStatus status) {
        return savePendingMediaRecord(message, qiniuUrl, null, MEDIA_TYPE_IMAGE, status);
    }

    /**
     * 写入 wecom_pending_image 暂存记录（通用，支持图片和视频），msg_id 唯一索引幂等保护
     */
    private WecomPendingImagePO savePendingMediaRecord(WecomCallbackMessageDTO message,
                                                        String qiniuUrl,
                                                        String thumbUrl,
                                                        String mediaType,
                                                        WecomPendingImageStatus status) {
        try {
            int windowMinutes = wecomProperties.getImage() != null
                    ? wecomProperties.getImage().getAssociationWindowMinutes()
                    : 5;

            WecomPendingImagePO po = new WecomPendingImagePO();
            po.setChatId(message.getChatId());
            po.setFromUserId(message.getFromWecomUserid());
            po.setMsgId(message.getMsgId());
            po.setMediaId(message.getMediaId());
            po.setPicUrl(message.getPicUrl());
            po.setQiniuUrl(qiniuUrl);
            po.setMediaType(mediaType);
            po.setThumbUrl(thumbUrl);
            po.setStatus(status.getCode());
            po.setExpireTime(minutesAfter(new Date(), windowMinutes));
            pendingImageMapper.insert(po);
            return po;
        } catch (Exception e) {
            log.warn("wecom_pending_image 写入失败（可能是重复消息幂等）: msgId={}, error={}",
                    message.getMsgId(), e.getMessage());
            return null;
        }
    }

    /**
     * 更新暂存图片状态
     */
    private void updatePendingStatus(Long pendingId, Long ticketId, WecomPendingImageStatus status) {
        LambdaUpdateWrapper<WecomPendingImagePO> update = new LambdaUpdateWrapper<WecomPendingImagePO>()
                .eq(WecomPendingImagePO::getId, pendingId)
                .set(WecomPendingImagePO::getStatus, status.getCode())
                .set(WecomPendingImagePO::getTicketId, ticketId);
        pendingImageMapper.update(null, update);
    }

    /**
     * 创建 ticket_attachment 记录（企微来源），根据 mediaType 设置对应的文件名与 MIME 类型
     */
    private void createAttachmentRecord(Long ticketId, String qiniuUrl, String wecomMsgId, String mediaType) {
        TicketAttachmentPO attachment = new TicketAttachmentPO();
        attachment.setTicketId(ticketId);
        attachment.setFilePath(qiniuUrl);
        attachment.setFileSize(0L);
        attachment.setUploadedBy(0L);
        attachment.setSource(AttachmentSource.WECOM_BOT.getCode());
        attachment.setWecomMsgId(wecomMsgId);
        if (MEDIA_TYPE_VIDEO.equalsIgnoreCase(mediaType)) {
            attachment.setFileName("企微视频");
            attachment.setFileType("video/mp4");
        } else {
            attachment.setFileName("企微截图");
            attachment.setFileType("image/jpeg");
        }
        attachmentMapper.insert(attachment);
    }

    /**
     * 统计工单已有附件数量
     */
    private int countExistingAttachments(Long ticketId) {
        return Math.toIntExact(attachmentMapper.selectCount(
                new LambdaQueryWrapper<TicketAttachmentPO>()
                        .eq(TicketAttachmentPO::getTicketId, ticketId)
        ));
    }

    /**
     * 发送图片已接收提示（基于 chatId+fromUserId 去重，5 分钟内不重复提示）
     */
    private void sendPendingNotification(String chatId, String fromUserId, String responseUrl) {
        String dedupKey = RedisKeyConstants.WECOM_IMAGE_NOTIFY_DEDUP_PREFIX + chatId + ":" + fromUserId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1",
                wecomProperties.getImage().getAssociationWindowMinutes(), TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            return;
        }
        String notifyMsg = "📷 图片已接收，请在" + wecomProperties.getImage().getAssociationWindowMinutes()
                + "分钟内补充文字描述，图片将自动关联到新工单。";
        if (responseUrl != null && !responseUrl.trim().isEmpty()) {
            try {
                wecomClient.sendAibotReply(responseUrl, notifyMsg);
            } catch (Exception e) {
                log.warn("企微图片接收提示发送失败: error={}", e.getMessage());
            }
        }
    }

    /**
     * 将企微 UserId 解析为系统用户 ID，用于 Rule A 关联时精确过滤创建人。
     * 若未找到对应系统账号则返回 null，调用方降级为仅按 chatId 匹配。
     */
    private Long resolveCreatorId(String wecomUserId) {
        if (wecomUserId == null || wecomUserId.trim().isEmpty()) {
            return null;
        }
        SysUserPO user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getWecomUserid, wecomUserId.trim())
                        .last("LIMIT 1")
        );
        return user != null ? user.getId() : null;
    }

    private Date minutesAfter(Date base, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    private Date minutesBefore(Date base, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.MINUTE, -minutes);
        return cal.getTime();
    }
}

package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.common.enums.AttachmentSource;
import com.miduo.cloud.ticket.common.enums.TicketSource;
import com.miduo.cloud.ticket.common.enums.WecomImageTimeoutStrategy;
import com.miduo.cloud.ticket.common.enums.WecomPendingImageStatus;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketAttachmentMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketAttachmentPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomPendingImageMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomPendingImagePO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 企微图片暂存超时清理服务
 * Task024：处理超时未关联图片的三种策略（CREATE_TICKET / EXPIRE / NOTIFY_USER）
 */
@Service
public class WecomPendingImageCleanupService {

    private static final Logger log = LoggerFactory.getLogger(WecomPendingImageCleanupService.class);

    private static final String IMAGE_TICKET_TITLE = "图片工单（待补充描述）";

    private final WecomPendingImageMapper pendingImageMapper;
    private final TicketApplicationService ticketApplicationService;
    private final TicketAttachmentMapper attachmentMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final WecomProperties wecomProperties;
    private final WecomClient wecomClient;

    public WecomPendingImageCleanupService(WecomPendingImageMapper pendingImageMapper,
                                           TicketApplicationService ticketApplicationService,
                                           TicketAttachmentMapper attachmentMapper,
                                           TicketCategoryMapper ticketCategoryMapper,
                                           SysUserMapper sysUserMapper,
                                           WecomProperties wecomProperties,
                                           WecomClient wecomClient) {
        this.pendingImageMapper = pendingImageMapper;
        this.ticketApplicationService = ticketApplicationService;
        this.attachmentMapper = attachmentMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.wecomProperties = wecomProperties;
        this.wecomClient = wecomClient;
    }

    /**
     * 处理超时未关联的 PENDING 图片记录
     * 幂等执行，每批最多处理 200 条
     */
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredPendingImages() {
        WecomProperties.ImageConfig config = wecomProperties.getImage();
        if (config == null || !config.isEnabled()) {
            return;
        }

        List<WecomPendingImagePO> expiredList = pendingImageMapper.selectExpiredPending(new Date());
        if (expiredList == null || expiredList.isEmpty()) {
            return;
        }

        WecomImageTimeoutStrategy strategy = WecomImageTimeoutStrategy.fromCode(config.getTimeoutStrategy());
        log.info("开始处理超时图片暂存记录: count={}, strategy={}", expiredList.size(), strategy.getCode());

        for (WecomPendingImagePO pending : expiredList) {
            try {
                processOne(pending, strategy, config);
            } catch (Exception e) {
                log.error("处理超时图片记录失败: id={}, msgId={}", pending.getId(), pending.getMsgId(), e);
            }
        }

        log.info("超时图片暂存记录处理完成: count={}", expiredList.size());
    }

    private void processOne(WecomPendingImagePO pending, WecomImageTimeoutStrategy strategy,
                             WecomProperties.ImageConfig config) {
        switch (strategy) {
            case CREATE_TICKET:
                handleCreateTicket(pending);
                break;
            case EXPIRE:
                updateStatus(pending.getId(), null, WecomPendingImageStatus.EXPIRED);
                log.info("超时图片标记为EXPIRED: id={}, msgId={}", pending.getId(), pending.getMsgId());
                break;
            case NOTIFY_USER:
                handleNotifyUser(pending, config);
                break;
            default:
                handleCreateTicket(pending);
        }
    }

    private void handleCreateTicket(WecomPendingImagePO pending) {
        if (pending.getQiniuUrl() == null || pending.getQiniuUrl().trim().isEmpty()) {
            updateStatus(pending.getId(), null, WecomPendingImageStatus.EXPIRED);
            log.warn("超时图片无七牛URL，标记EXPIRED: id={}", pending.getId());
            return;
        }

        TicketCategoryPO defaultCategory = ticketCategoryMapper.selectOne(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .eq(TicketCategoryPO::getIsActive, 1)
                        .orderByAsc(TicketCategoryPO::getLevel)
                        .orderByAsc(TicketCategoryPO::getSortOrder)
                        .last("LIMIT 1")
        );
        if (defaultCategory == null) {
            log.warn("系统无可用分类，无法创建图片工单: id={}", pending.getId());
            updateStatus(pending.getId(), null, WecomPendingImageStatus.EXPIRED);
            return;
        }

        SysUserPO creator = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getAccountStatus, 1)
                        .orderByAsc(SysUserPO::getId)
                        .last("LIMIT 1")
        );
        if (creator == null) {
            log.warn("系统无可用账号，无法创建图片工单: id={}", pending.getId());
            updateStatus(pending.getId(), null, WecomPendingImageStatus.EXPIRED);
            return;
        }

        TicketCreateInput input = new TicketCreateInput();
        input.setTitle(IMAGE_TICKET_TITLE);
        input.setDescription("企微图片工单，来自用户：" + pending.getFromUserId()
                + "（原消息ID：" + pending.getMsgId() + "）");
        input.setCategoryId(defaultCategory.getId());
        input.setPriority("medium");
        input.setSource(TicketSource.WECOM_BOT.getCode());
        input.setSourceChatId(pending.getChatId());

        Long ticketId = ticketApplicationService.createTicket(input, creator.getId());

        TicketAttachmentPO attachment = new TicketAttachmentPO();
        attachment.setTicketId(ticketId);
        attachment.setFileName("企微截图");
        attachment.setFilePath(pending.getQiniuUrl());
        attachment.setFileSize(0L);
        attachment.setFileType("image/jpeg");
        attachment.setUploadedBy(0L);
        attachment.setSource(AttachmentSource.WECOM_BOT.getCode());
        attachment.setWecomMsgId(pending.getMsgId());
        attachmentMapper.insert(attachment);

        updateStatus(pending.getId(), ticketId, WecomPendingImageStatus.LINKED);
        log.info("超时图片静默建单成功: pendingId={}, ticketId={}", pending.getId(), ticketId);
    }

    private void handleNotifyUser(WecomPendingImagePO pending, WecomProperties.ImageConfig config) {
        String notifyMsg = "📷 您有图片待关联工单，请补充文字描述，图片将自动关联到新工单。";
        if (pending.getFromUserId() != null && !pending.getFromUserId().trim().isEmpty()) {
            try {
                wecomClient.sendTextMessage(pending.getFromUserId(), notifyMsg);
            } catch (Exception e) {
                log.warn("超时图片通知用户发送失败: id={}, fromUserId={}", pending.getId(), pending.getFromUserId());
            }
        }
        int windowMinutes = config.getAssociationWindowMinutes();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, windowMinutes);
        LambdaUpdateWrapper<WecomPendingImagePO> update = new LambdaUpdateWrapper<WecomPendingImagePO>()
                .eq(WecomPendingImagePO::getId, pending.getId())
                .set(WecomPendingImagePO::getExpireTime, cal.getTime());
        pendingImageMapper.update(null, update);
        log.info("超时图片已通知用户并重置expireTime: id={}", pending.getId());
    }

    private void updateStatus(Long pendingId, Long ticketId, WecomPendingImageStatus status) {
        LambdaUpdateWrapper<WecomPendingImagePO> update = new LambdaUpdateWrapper<WecomPendingImagePO>()
                .eq(WecomPendingImagePO::getId, pendingId)
                .set(WecomPendingImagePO::getStatus, status.getCode())
                .set(WecomPendingImagePO::getTicketId, ticketId);
        pendingImageMapper.update(null, update);
    }
}

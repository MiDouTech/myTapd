package com.miduo.cloud.ticket.application.ticket;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.*;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.util.TicketNoGenerator;
import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.application.workflow.WorkflowApplicationService;
import com.miduo.cloud.ticket.entity.dto.ticket.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportTicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportTicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TicketApplicationService.class);

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private TicketCategoryMapper categoryMapper;

    @Resource
    private TicketTemplateMapper templateMapper;

    @Resource
    private TicketLogMapper logMapper;

    @Resource
    private TicketCommentMapper commentMapper;

    @Resource
    private TicketAttachmentMapper attachmentMapper;

    @Resource
    private TicketFollowerMapper followerMapper;

    @Resource
    private TicketCustomFieldMapper customFieldMapper;

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private WorkflowApplicationService workflowService;

    @Resource
    private BugReportMapper bugReportMapper;

    @Resource
    private BugReportTicketMapper bugReportTicketMapper;

    @Resource
    private TicketTimeTrackApplicationService ticketTimeTrackService;

    @Resource
    private TicketBugApplicationService ticketBugApplicationService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketCreateInput input, Long currentUserId) {
        TicketCategoryPO category = categoryMapper.selectById(input.getCategoryId());
        if (category == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "工单分类不存在");
        }

        Long workflowId = category.getWorkflowId();
        Long templateId = category.getTemplateId();
        String initialStatus = workflowService.getInitialStatus(workflowId);

        TicketPO ticket = new TicketPO();
        ticket.setTicketNo(TicketNoGenerator.generateTicketNo());
        ticket.setTitle(input.getTitle());
        ticket.setDescription(input.getDescription());
        ticket.setCategoryId(input.getCategoryId());
        ticket.setTemplateId(templateId);
        ticket.setWorkflowId(workflowId);
        ticket.setPriority(input.getPriority());
        ticket.setStatus(initialStatus);
        ticket.setCreatorId(currentUserId);
        ticket.setAssigneeId(input.getAssigneeId());
        ticket.setSource(input.getSource() != null ? input.getSource() : TicketSource.WEB.getCode());
        ticket.setSourceChatId(input.getSourceChatId());
        ticket.setExpectedTime(input.getExpectedTime());

        if (input.getCustomFields() != null && !input.getCustomFields().isEmpty()) {
            ticket.setCustomFields(JSON.toJSONString(input.getCustomFields()));
        }

        ticketMapper.insert(ticket);

        if (input.getCustomFields() != null && !input.getCustomFields().isEmpty()) {
            List<TicketCustomFieldPO> customFields = new ArrayList<>();
            for (Map.Entry<String, String> entry : input.getCustomFields().entrySet()) {
                TicketCustomFieldPO cf = new TicketCustomFieldPO();
                cf.setTicketId(ticket.getId());
                cf.setFieldKey(entry.getKey());
                cf.setFieldValue(entry.getValue());
                customFields.add(cf);
            }
            for (TicketCustomFieldPO cf : customFields) {
                customFieldMapper.insert(cf);
            }
        }

        recordLog(ticket.getId(), currentUserId, TicketAction.CREATE.getCode(),
                null, ticket.getStatus(), "创建工单: " + ticket.getTicketNo());
        ticketTimeTrackService.recordCreate(ticket.getId(), currentUserId, ticket.getStatus(),
                "创建工单: " + ticket.getTicketNo());

        eventPublisher.publishEvent(new TicketCreatedEvent(ticket.getId(), ticket.getCategoryId(), ticket.getPriority()));
        if (ticket.getAssigneeId() != null) {
            eventPublisher.publishEvent(new TicketAssignedEvent(
                    ticket.getId(), ticket.getAssigneeId(), null, currentUserId, "CREATE_ASSIGN"));
        }

        log.info("工单创建成功: ticketNo={}, creatorId={}", ticket.getTicketNo(), currentUserId);
        return ticket.getId();
    }

    public PageOutput<TicketListOutput> getTicketPage(TicketPageInput input, Long currentUserId) {
        Page<TicketPO> page = new Page<>(input.getPageNum(), input.getPageSize());

        IPage<TicketPO> result = ticketMapper.selectTicketPage(
                page,
                input.getView(),
                currentUserId,
                input.getTicketNo(),
                input.getTitle(),
                input.getCategoryId(),
                input.getStatus(),
                input.getPriority(),
                input.getCreatorId(),
                input.getAssigneeId(),
                input.getCreateTimeStart(),
                input.getCreateTimeEnd(),
                input.getOrderBy(),
                input.isAsc()
        );

        List<TicketPO> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            return PageOutput.empty(input.getPageNum(), input.getPageSize());
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (TicketPO t : records) {
            if (t.getCreatorId() != null) userIds.add(t.getCreatorId());
            if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
            if (t.getCategoryId() != null) categoryIds.add(t.getCategoryId());
        }

        Map<Long, String> userNameMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUserPO> users = userMapper.selectBatchIds(userIds);
            userNameMap = users.stream().collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName));
        }

        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            List<TicketCategoryPO> categories = categoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(TicketCategoryPO::getId, TicketCategoryPO::getName));
        }

        Map<Long, String> finalUserNameMap = userNameMap;
        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        List<TicketListOutput> outputs = records.stream()
                .map(po -> convertToListOutput(po, finalUserNameMap, finalCategoryNameMap))
                .collect(Collectors.toList());

        return PageOutput.of(outputs, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    public TicketDetailOutput getTicketDetail(Long id, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return buildDetailOutput(ticket, currentUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long ticketId, TicketAssignInput input, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        SysUserPO assignee = userMapper.selectById(input.getAssigneeId());
        if (assignee == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "处理人不存在");
        }

        Long oldAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(input.getAssigneeId());
        ticketMapper.updateById(ticket);

        String oldValue = oldAssigneeId != null ? String.valueOf(oldAssigneeId) : "";
        String newValue = String.valueOf(input.getAssigneeId());
        recordLog(ticketId, currentUserId, TicketAction.ASSIGN.getCode(),
                oldValue, newValue, input.getRemark());
        ticketTimeTrackService.recordAssign(ticketId, currentUserId, oldAssigneeId, input.getAssigneeId(),
                ticket.getStatus(), ticket.getStatus(), input.getRemark());

        eventPublisher.publishEvent(new TicketAssignedEvent(ticketId, input.getAssigneeId(),
                oldAssigneeId, currentUserId, "MANUAL_ASSIGN"));

        log.info("工单分派: ticketId={}, assigneeId={}", ticketId, input.getAssigneeId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long ticketId, TicketProcessInput input, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        String fromStatus = ticket.getStatus();
        String toStatus = input.getTargetStatus();
        Long oldAssigneeId = ticket.getAssigneeId();

        workflowService.validateTransition(ticket.getWorkflowId(), fromStatus, toStatus);

        ticket.setStatus(toStatus);

        if (input.getTargetUserId() != null) {
            ticket.setAssigneeId(input.getTargetUserId());
        }
        Long newAssigneeId = ticket.getAssigneeId();

        if (workflowService.isTerminalStatus(ticket.getWorkflowId(), toStatus)) {
            if ("COMPLETED".equals(toStatus)) {
                ticket.setResolvedAt(new Date());
            }
            ticket.setClosedAt(new Date());
        }

        ticketMapper.updateById(ticket);

        if ("COMPLETED".equalsIgnoreCase(toStatus) || "CLOSED".equalsIgnoreCase(toStatus)) {
            eventPublisher.publishEvent(new TicketCompletedEvent(ticketId, toStatus, currentUserId, new Date()));
        }

        recordLog(ticketId, currentUserId, TicketAction.PROCESS.getCode(),
                fromStatus, toStatus, input.getRemark());
        String transitionAction = ticketTimeTrackService.resolveTransitionAction(fromStatus, toStatus);
        ticketTimeTrackService.recordStatusTrack(ticketId, currentUserId, transitionAction,
                fromStatus, toStatus, oldAssigneeId, newAssigneeId, input.getRemark());

        if (input.getRemark() != null && !input.getRemark().isEmpty()) {
            recordOperationComment(ticketId, currentUserId, input.getRemark());
        }

        eventPublisher.publishEvent(new TicketStatusChangedEvent(ticketId, fromStatus, toStatus, currentUserId));
        if (input.getTargetUserId() != null && !input.getTargetUserId().equals(oldAssigneeId)) {
            eventPublisher.publishEvent(new TicketAssignedEvent(
                    ticketId, input.getTargetUserId(), oldAssigneeId, currentUserId, "PROCESS_ASSIGN"));
        }

        log.info("工单处理: ticketId={}, {} -> {}", ticketId, fromStatus, toStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long ticketId, TicketCloseInput input, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        String fromStatus = ticket.getStatus();
        Long assigneeId = ticket.getAssigneeId();
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(new Date());
        ticketMapper.updateById(ticket);

        eventPublisher.publishEvent(new TicketCompletedEvent(ticketId, "CLOSED", currentUserId, new Date()));

        recordLog(ticketId, currentUserId, TicketAction.CLOSE.getCode(),
                fromStatus, "CLOSED",
                input != null ? input.getRemark() : null);
        ticketTimeTrackService.recordStatusTrack(ticketId, currentUserId, TicketAction.COMPLETE.getCode(),
                fromStatus, "CLOSED", assigneeId, assigneeId, input != null ? input.getRemark() : null);

        eventPublisher.publishEvent(new TicketStatusChangedEvent(ticketId, fromStatus, "CLOSED", currentUserId));

        log.info("工单关闭: ticketId={}", ticketId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void followTicket(Long ticketId, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        LambdaQueryWrapper<TicketFollowerPO> query = new LambdaQueryWrapper<>();
        query.eq(TicketFollowerPO::getTicketId, ticketId)
                .eq(TicketFollowerPO::getUserId, currentUserId);
        Long count = followerMapper.selectCount(query);
        if (count != null && count > 0) {
            return;
        }

        TicketFollowerPO follower = new TicketFollowerPO();
        follower.setTicketId(ticketId);
        follower.setUserId(currentUserId);
        followerMapper.insert(follower);

        recordLog(ticketId, currentUserId, TicketAction.FOLLOW.getCode(), null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfollowTicket(Long ticketId, Long currentUserId) {
        LambdaQueryWrapper<TicketFollowerPO> query = new LambdaQueryWrapper<>();
        query.eq(TicketFollowerPO::getTicketId, ticketId)
                .eq(TicketFollowerPO::getUserId, currentUserId);
        followerMapper.delete(query);

        recordLog(ticketId, currentUserId, TicketAction.UNFOLLOW.getCode(), null, null, null);
    }

    private TicketDetailOutput buildDetailOutput(TicketPO ticket, Long currentUserId) {
        TicketDetailOutput output = new TicketDetailOutput();
        output.setId(ticket.getId());
        output.setTicketNo(ticket.getTicketNo());
        output.setTitle(ticket.getTitle());
        output.setDescription(ticket.getDescription());
        output.setCategoryId(ticket.getCategoryId());
        output.setTemplateId(ticket.getTemplateId());
        output.setWorkflowId(ticket.getWorkflowId());
        output.setPriority(ticket.getPriority());
        output.setStatus(ticket.getStatus());
        output.setCreatorId(ticket.getCreatorId());
        output.setAssigneeId(ticket.getAssigneeId());
        output.setSource(ticket.getSource());
        output.setExpectedTime(ticket.getExpectedTime());
        output.setResolvedAt(ticket.getResolvedAt());
        output.setClosedAt(ticket.getClosedAt());
        output.setCreateTime(ticket.getCreateTime());
        output.setUpdateTime(ticket.getUpdateTime());

        Priority priority = Priority.fromCode(ticket.getPriority());
        if (priority != null) {
            output.setPriorityLabel(priority.getLabel());
        }

        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        if (status != null) {
            output.setStatusLabel(status.getLabel());
        }

        TicketSource source = TicketSource.fromCode(ticket.getSource());
        if (source != null) {
            output.setSourceLabel(source.getLabel());
        }

        Set<Long> userIds = new HashSet<>();
        if (ticket.getCreatorId() != null) userIds.add(ticket.getCreatorId());
        if (ticket.getAssigneeId() != null) userIds.add(ticket.getAssigneeId());

        if (ticket.getCategoryId() != null) {
            TicketCategoryPO category = categoryMapper.selectById(ticket.getCategoryId());
            if (category != null) {
                output.setCategoryName(category.getName());
                output.setCategoryFullPath(category.getPath());
            }
        }

        if (ticket.getTemplateId() != null) {
            TicketTemplatePO template = templateMapper.selectById(ticket.getTemplateId());
            if (template != null) {
                output.setTemplateName(template.getName());
            }
        }

        if (ticket.getCustomFields() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> cf = JSON.parseObject(ticket.getCustomFields(), Map.class);
                output.setCustomFields(cf);
            } catch (Exception e) {
                log.warn("解析自定义字段失败: ticketId={}", ticket.getId(), e);
            }
        }

        output.setBugCustomerInfo(ticketBugApplicationService.getCustomerInfo(ticket.getId()));
        output.setBugTestInfo(ticketBugApplicationService.getTestInfo(ticket.getId()));
        output.setBugDevInfo(ticketBugApplicationService.getDevInfo(ticket.getId()));

        List<TicketAttachmentPO> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentPO>()
                        .eq(TicketAttachmentPO::getTicketId, ticket.getId())
                        .orderByDesc(TicketAttachmentPO::getCreateTime)
        );
        if (attachments != null) {
            for (TicketAttachmentPO a : attachments) {
                if (a.getUploadedBy() != null) userIds.add(a.getUploadedBy());
            }
        }

        List<TicketCommentPO> comments = commentMapper.selectList(
                new LambdaQueryWrapper<TicketCommentPO>()
                        .eq(TicketCommentPO::getTicketId, ticket.getId())
                        .orderByDesc(TicketCommentPO::getCreateTime)
        );
        if (comments != null) {
            for (TicketCommentPO c : comments) {
                if (c.getUserId() != null) userIds.add(c.getUserId());
            }
        }

        List<TicketLogPO> logs = logMapper.selectList(
                new LambdaQueryWrapper<TicketLogPO>()
                        .eq(TicketLogPO::getTicketId, ticket.getId())
                        .orderByDesc(TicketLogPO::getCreateTime)
        );
        if (logs != null) {
            for (TicketLogPO l : logs) {
                if (l.getUserId() != null) userIds.add(l.getUserId());
            }
        }

        Map<Long, SysUserPO> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUserPO> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(SysUserPO::getId, u -> u));
        }

        SysUserPO creator = userMap.get(ticket.getCreatorId());
        if (creator != null) {
            output.setCreatorName(creator.getName());
        }
        SysUserPO assignee = userMap.get(ticket.getAssigneeId());
        if (assignee != null) {
            output.setAssigneeName(assignee.getName());
        }

        if (attachments != null) {
            Map<Long, SysUserPO> finalUserMap = userMap;
            output.setAttachments(attachments.stream().map(a -> {
                TicketDetailOutput.AttachmentOutput ao = new TicketDetailOutput.AttachmentOutput();
                ao.setId(a.getId());
                ao.setFileName(a.getFileName());
                ao.setFilePath(a.getFilePath());
                ao.setFileSize(a.getFileSize());
                ao.setFileType(a.getFileType());
                ao.setUploadedBy(a.getUploadedBy());
                SysUserPO uploader = finalUserMap.get(a.getUploadedBy());
                if (uploader != null) {
                    ao.setUploadedByName(uploader.getName());
                }
                ao.setCreateTime(a.getCreateTime());
                return ao;
            }).collect(Collectors.toList()));
        }

        if (comments != null) {
            Map<Long, SysUserPO> finalUserMap2 = userMap;
            output.setComments(comments.stream().map(c -> {
                TicketDetailOutput.CommentOutput co = new TicketDetailOutput.CommentOutput();
                co.setId(c.getId());
                co.setUserId(c.getUserId());
                SysUserPO commentUser = finalUserMap2.get(c.getUserId());
                if (commentUser != null) {
                    co.setUserName(commentUser.getName());
                    co.setUserAvatar(commentUser.getAvatarUrl());
                }
                co.setContent(c.getContent());
                co.setType(c.getType());
                co.setCreateTime(c.getCreateTime());
                return co;
            }).collect(Collectors.toList()));
        }

        if (logs != null) {
            Map<Long, SysUserPO> finalUserMap3 = userMap;
            output.setLogs(logs.stream().map(l -> {
                TicketDetailOutput.LogOutput lo = new TicketDetailOutput.LogOutput();
                lo.setId(l.getId());
                lo.setUserId(l.getUserId());
                SysUserPO logUser = finalUserMap3.get(l.getUserId());
                if (logUser != null) {
                    lo.setUserName(logUser.getName());
                }
                lo.setAction(l.getAction());
                TicketAction action = TicketAction.fromCode(l.getAction());
                if (action != null) {
                    lo.setActionLabel(action.getLabel());
                }
                lo.setOldValue(l.getOldValue());
                lo.setNewValue(l.getNewValue());
                lo.setRemark(l.getRemark());
                lo.setCreateTime(l.getCreateTime());
                return lo;
            }).collect(Collectors.toList()));
        }

        List<BugReportTicketPO> bugReportTickets = bugReportTicketMapper.selectList(
                new LambdaQueryWrapper<BugReportTicketPO>()
                        .eq(BugReportTicketPO::getTicketId, ticket.getId())
                        .orderByDesc(BugReportTicketPO::getCreateTime)
        );
        if (bugReportTickets != null && !bugReportTickets.isEmpty()) {
            Set<Long> reportIds = bugReportTickets.stream()
                    .map(BugReportTicketPO::getReportId)
                    .collect(Collectors.toSet());
            List<BugReportPO> reportList = bugReportMapper.selectBatchIds(reportIds);
            Map<Long, BugReportPO> reportMap = reportList.stream()
                    .collect(Collectors.toMap(BugReportPO::getId, r -> r));

            output.setBugReports(bugReportTickets.stream().map(rt -> {
                TicketDetailOutput.BugReportOutput bo = new TicketDetailOutput.BugReportOutput();
                BugReportPO report = reportMap.get(rt.getReportId());
                if (report != null) {
                    bo.setId(report.getId());
                    bo.setReportNo(report.getReportNo());
                    bo.setStatus(report.getStatus());
                    BugReportStatus brStatus = BugReportStatus.fromCode(report.getStatus());
                    bo.setStatusLabel(brStatus != null ? brStatus.getLabel() : report.getStatus());
                    bo.setCreateTime(report.getCreateTime());
                } else {
                    bo.setId(rt.getReportId());
                }
                bo.setIsAutoCreated(rt.getIsAutoCreated());
                return bo;
            }).collect(Collectors.toList()));
        } else {
            output.setBugReports(Collections.emptyList());
        }

        if (currentUserId != null) {
            Long followCount = followerMapper.selectCount(
                    new LambdaQueryWrapper<TicketFollowerPO>()
                            .eq(TicketFollowerPO::getTicketId, ticket.getId())
                            .eq(TicketFollowerPO::getUserId, currentUserId)
            );
            output.setIsFollowed(followCount != null && followCount > 0);
        }

        return output;
    }

    private TicketListOutput convertToListOutput(TicketPO po,
                                                  Map<Long, String> userNameMap,
                                                  Map<Long, String> categoryNameMap) {
        TicketListOutput output = new TicketListOutput();
        output.setId(po.getId());
        output.setTicketNo(po.getTicketNo());
        output.setTitle(po.getTitle());
        output.setCategoryId(po.getCategoryId());
        output.setCategoryName(categoryNameMap.get(po.getCategoryId()));
        output.setPriority(po.getPriority());
        output.setStatus(po.getStatus());
        output.setCreatorId(po.getCreatorId());
        output.setCreatorName(userNameMap.get(po.getCreatorId()));
        output.setAssigneeId(po.getAssigneeId());
        output.setAssigneeName(userNameMap.get(po.getAssigneeId()));
        output.setSource(po.getSource());
        output.setExpectedTime(po.getExpectedTime());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        output.setResolvedAt(po.getResolvedAt());
        output.setClosedAt(po.getClosedAt());

        Priority priority = Priority.fromCode(po.getPriority());
        if (priority != null) {
            output.setPriorityLabel(priority.getLabel());
        }
        TicketStatus status = TicketStatus.fromCode(po.getStatus());
        if (status != null) {
            output.setStatusLabel(status.getLabel());
        }
        TicketSource source = TicketSource.fromCode(po.getSource());
        if (source != null) {
            output.setSourceLabel(source.getLabel());
        }

        return output;
    }

    private void recordLog(Long ticketId, Long userId, String action,
                           String oldValue, String newValue, String remark) {
        TicketLogPO logPO = new TicketLogPO();
        logPO.setTicketId(ticketId);
        logPO.setUserId(userId);
        logPO.setAction(action);
        logPO.setOldValue(oldValue);
        logPO.setNewValue(newValue);
        logPO.setRemark(remark);
        logMapper.insert(logPO);
    }

    private void recordOperationComment(Long ticketId, Long userId, String content) {
        TicketCommentPO comment = new TicketCommentPO();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setType(CommentType.OPERATION.getCode());
        commentMapper.insert(comment);
    }
}

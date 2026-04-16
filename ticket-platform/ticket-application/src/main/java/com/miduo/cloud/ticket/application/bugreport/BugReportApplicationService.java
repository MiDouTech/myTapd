package com.miduo.cloud.ticket.application.bugreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.NotificationOrchestrator;
import com.miduo.cloud.ticket.application.notification.WecomGroupPushService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.BugReportStatus;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.util.TicketNoGenerator;
import com.miduo.cloud.ticket.entity.dto.bugreport.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bug简报应用服务
 */
@Service
public class BugReportApplicationService extends BaseApplicationService {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final BugReportMapper bugReportMapper;
    private final BugReportResponsibleMapper bugReportResponsibleMapper;
    private final BugReportTicketMapper bugReportTicketMapper;
    private final BugReportLogMapper bugReportLogMapper;
    private final BugReportAttachmentMapper bugReportAttachmentMapper;
    private final TicketMapper ticketMapper;
    private final TicketBugInfoMapper ticketBugInfoMapper;
    private final TicketBugTestInfoMapper ticketBugTestInfoMapper;
    private final TicketBugDevInfoMapper ticketBugDevInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationOrchestrator notificationOrchestrator;
    private final WecomGroupPushService wecomGroupPushService;
    private final SystemConfigMapper systemConfigMapper;

    public BugReportApplicationService(BugReportMapper bugReportMapper,
                                       BugReportResponsibleMapper bugReportResponsibleMapper,
                                       BugReportTicketMapper bugReportTicketMapper,
                                       BugReportLogMapper bugReportLogMapper,
                                       BugReportAttachmentMapper bugReportAttachmentMapper,
                                       TicketMapper ticketMapper,
                                       TicketBugInfoMapper ticketBugInfoMapper,
                                       TicketBugTestInfoMapper ticketBugTestInfoMapper,
                                       TicketBugDevInfoMapper ticketBugDevInfoMapper,
                                       SysUserMapper sysUserMapper,
                                       NotificationOrchestrator notificationOrchestrator,
                                       WecomGroupPushService wecomGroupPushService,
                                       SystemConfigMapper systemConfigMapper) {
        this.bugReportMapper = bugReportMapper;
        this.bugReportResponsibleMapper = bugReportResponsibleMapper;
        this.bugReportTicketMapper = bugReportTicketMapper;
        this.bugReportLogMapper = bugReportLogMapper;
        this.bugReportAttachmentMapper = bugReportAttachmentMapper;
        this.ticketMapper = ticketMapper;
        this.ticketBugInfoMapper = ticketBugInfoMapper;
        this.ticketBugTestInfoMapper = ticketBugTestInfoMapper;
        this.ticketBugDevInfoMapper = ticketBugDevInfoMapper;
        this.sysUserMapper = sysUserMapper;
        this.notificationOrchestrator = notificationOrchestrator;
        this.wecomGroupPushService = wecomGroupPushService;
        this.systemConfigMapper = systemConfigMapper;
    }

    public PageOutput<BugReportPageOutput> page(BugReportPageInput input) {
        Page<BugReportPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        LambdaQueryWrapper<BugReportPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(input.getReportNo())) {
            wrapper.likeRight(BugReportPO::getReportNo, input.getReportNo().trim());
        }
        if (StringUtils.hasText(input.getStatus())) {
            wrapper.eq(BugReportPO::getStatus, input.getStatus().trim());
        }
        if (StringUtils.hasText(input.getDefectCategory())) {
            wrapper.eq(BugReportPO::getDefectCategory, input.getDefectCategory().trim());
        }
        if (input.getReviewerId() != null) {
            wrapper.eq(BugReportPO::getReviewerId, input.getReviewerId());
        }

        Date createStart = parseDateTime(input.getCreateTimeStart(), false);
        Date createEnd = parseDateTime(input.getCreateTimeEnd(), true);
        if (createStart != null) {
            wrapper.ge(BugReportPO::getCreateTime, createStart);
        }
        if (createEnd != null) {
            wrapper.le(BugReportPO::getCreateTime, createEnd);
        }

        Set<Long> filteredReportIds = null;
        if (input.getResponsibleUserId() != null) {
            filteredReportIds = findReportIdsByResponsible(input.getResponsibleUserId());
        }
        if (input.getTicketId() != null) {
            Set<Long> ticketFilteredIds = findReportIdsByTicket(input.getTicketId());
            if (filteredReportIds == null) {
                filteredReportIds = ticketFilteredIds;
            } else {
                filteredReportIds.retainAll(ticketFilteredIds);
            }
        }
        if (filteredReportIds != null) {
            if (filteredReportIds.isEmpty()) {
                return PageOutput.empty(input.getPageNum(), input.getPageSize());
            }
            wrapper.in(BugReportPO::getId, filteredReportIds);
        }

        if ("update_time".equals(input.getOrderBy())) {
            wrapper.orderBy(true, input.isAsc(), BugReportPO::getUpdateTime);
        } else if ("submitted_at".equals(input.getOrderBy())) {
            wrapper.orderBy(true, input.isAsc(), BugReportPO::getSubmittedAt);
        } else {
            wrapper.orderBy(true, input.isAsc(), BugReportPO::getCreateTime);
        }

        Page<BugReportPO> result = bugReportMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(result.getRecords())) {
            return PageOutput.empty(input.getPageNum(), input.getPageSize());
        }

        Set<Long> reviewerIds = result.getRecords().stream()
                .map(BugReportPO::getReviewerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> reviewerNameMap = getUserNameMap(reviewerIds);

        List<BugReportPageOutput> records = result.getRecords().stream()
                .map(item -> convertToPageOutput(item, reviewerNameMap))
                .collect(Collectors.toList());
        return PageOutput.of(records, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    public BugReportDetailOutput detail(Long id) {
        BugReportPO report = getReportById(id);
        BugReportDetailOutput output = new BugReportDetailOutput();
        output.setId(report.getId());
        output.setReportNo(report.getReportNo());
        output.setStatus(report.getStatus());
        BugReportStatus status = BugReportStatus.fromCode(report.getStatus());
        output.setStatusLabel(status != null ? status.getLabel() : report.getStatus());
        output.setProblemDesc(report.getProblemDesc());
        output.setLogicCauseLevel1(report.getLogicCauseLevel1());
        output.setLogicCauseLevel2(report.getLogicCauseLevel2());
        output.setLogicCauseDetail(report.getLogicCauseDetail());
        output.setDefectCategory(report.getDefectCategory());
        output.setIntroducedProject(report.getIntroducedProject());
        output.setStartDate(report.getStartDate());
        output.setResolveDate(report.getResolveDate());
        output.setTempResolveDate(report.getTempResolveDate());
        output.setSolution(report.getSolution());
        output.setTempSolution(report.getTempSolution());
        output.setImpactScope(report.getImpactScope());
        output.setSeverityLevel(report.getSeverityLevel());
        output.setReporterId(report.getReporterId());
        output.setReviewerId(report.getReviewerId());
        output.setRemark(report.getRemark());
        output.setSubmittedAt(report.getSubmittedAt());
        output.setReviewedAt(report.getReviewedAt());
        output.setReviewComment(report.getReviewComment());
        output.setCreatedByUserId(report.getCreatedByUserId());
        output.setCreateTime(report.getCreateTime());
        output.setUpdateTime(report.getUpdateTime());

        List<BugReportResponsiblePO> responsibleList = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>()
                        .eq(BugReportResponsiblePO::getReportId, id)
                        .orderByAsc(BugReportResponsiblePO::getCreateTime)
        );
        List<BugReportTicketPO> reportTicketList = bugReportTicketMapper.selectList(
                new LambdaQueryWrapper<BugReportTicketPO>()
                        .eq(BugReportTicketPO::getReportId, id)
                        .orderByAsc(BugReportTicketPO::getCreateTime)
        );
        List<BugReportLogPO> logList = bugReportLogMapper.selectList(
                new LambdaQueryWrapper<BugReportLogPO>()
                        .eq(BugReportLogPO::getReportId, id)
                        .orderByDesc(BugReportLogPO::getCreateTime)
        );
        List<BugReportAttachmentPO> attachmentList = bugReportAttachmentMapper.selectList(
                new LambdaQueryWrapper<BugReportAttachmentPO>()
                        .eq(BugReportAttachmentPO::getReportId, id)
                        .orderByDesc(BugReportAttachmentPO::getCreateTime)
        );

        Set<Long> userIds = new HashSet<>();
        if (report.getReporterId() != null) {
            userIds.add(report.getReporterId());
        }
        if (report.getReviewerId() != null) {
            userIds.add(report.getReviewerId());
        }
        for (BugReportResponsiblePO responsiblePO : responsibleList) {
            userIds.add(responsiblePO.getUserId());
        }
        for (BugReportLogPO logPO : logList) {
            userIds.add(logPO.getUserId());
        }
        for (BugReportAttachmentPO attachmentPO : attachmentList) {
            userIds.add(attachmentPO.getUploadedBy());
        }
        Map<Long, String> userNameMap = getUserNameMap(userIds);

        output.setReporterName(userNameMap.get(report.getReporterId()));
        output.setReviewerName(userNameMap.get(report.getReviewerId()));

        output.setResponsibleUsers(responsibleList.stream().map(item -> {
            BugReportDetailOutput.ResponsibleUserOutput responsibleUserOutput =
                    new BugReportDetailOutput.ResponsibleUserOutput();
            responsibleUserOutput.setUserId(item.getUserId());
            responsibleUserOutput.setUserName(userNameMap.get(item.getUserId()));
            return responsibleUserOutput;
        }).collect(Collectors.toList()));

        Set<Long> ticketIds = reportTicketList.stream().map(BugReportTicketPO::getTicketId).collect(Collectors.toSet());
        Map<Long, TicketPO> ticketMap = Collections.emptyMap();
        if (!ticketIds.isEmpty()) {
            List<TicketPO> tickets = ticketMapper.selectBatchIds(ticketIds);
            ticketMap = tickets.stream().collect(Collectors.toMap(TicketPO::getId, t -> t));
        }
        Map<Long, TicketPO> finalTicketMap = ticketMap;
        output.setTickets(reportTicketList.stream().map(item -> {
            BugReportDetailOutput.RelatedTicketOutput ticketOutput = new BugReportDetailOutput.RelatedTicketOutput();
            ticketOutput.setTicketId(item.getTicketId());
            ticketOutput.setIsAutoCreated(item.getIsAutoCreated());
            TicketPO ticketPO = finalTicketMap.get(item.getTicketId());
            if (ticketPO != null) {
                ticketOutput.setTicketNo(ticketPO.getTicketNo());
                ticketOutput.setTitle(ticketPO.getTitle());
                ticketOutput.setStatus(ticketPO.getStatus());
            }
            return ticketOutput;
        }).collect(Collectors.toList()));

        output.setLogs(logList.stream().map(item -> {
            BugReportDetailOutput.LogOutput logOutput = new BugReportDetailOutput.LogOutput();
            logOutput.setId(item.getId());
            logOutput.setUserId(item.getUserId());
            logOutput.setUserName(userNameMap.get(item.getUserId()));
            logOutput.setAction(item.getAction());
            logOutput.setOldStatus(item.getOldStatus());
            logOutput.setNewStatus(item.getNewStatus());
            logOutput.setRemark(item.getRemark());
            logOutput.setCreateTime(item.getCreateTime());
            return logOutput;
        }).collect(Collectors.toList()));

        output.setAttachments(attachmentList.stream().map(item -> {
            BugReportDetailOutput.AttachmentOutput attachmentOutput = new BugReportDetailOutput.AttachmentOutput();
            attachmentOutput.setId(item.getId());
            attachmentOutput.setFileName(item.getFileName());
            attachmentOutput.setFilePath(item.getFilePath());
            attachmentOutput.setFileSize(item.getFileSize());
            attachmentOutput.setUploadedBy(item.getUploadedBy());
            attachmentOutput.setUploadedByName(userNameMap.get(item.getUploadedBy()));
            attachmentOutput.setCreateTime(item.getCreateTime());
            return attachmentOutput;
        }).collect(Collectors.toList()));
        return output;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BugReportCreateInput input, Long currentUserId) {
        List<Long> ticketIds = distinctIds(input.getTicketIds());
        List<TicketPO> tickets = fetchAndValidateTickets(ticketIds);

        BugReportPO report = new BugReportPO();
        report.setReportNo(TicketNoGenerator.generateBugReportNo());
        report.setStatus(BugReportStatus.DRAFT.getCode());
        report.setCreatedByUserId(currentUserId);
        applyCreateInput(report, input);

        boolean autoPrefill = input.getAutoPrefill() == null || input.getAutoPrefill();
        if (autoPrefill) {
            applyAutoPrefill(report, tickets, ticketIds);
        }
        if (report.getReporterId() == null) {
            report.setReporterId(currentUserId);
        }
        bugReportMapper.insert(report);

        syncReportTickets(report.getId(), ticketIds, 0);
        List<Long> responsibleUserIds = mergeResponsibleUserIds(input.getResponsibleUserIds(), tickets);
        syncResponsibleUsers(report.getId(), responsibleUserIds);
        recordLog(report.getId(), currentUserId, "CREATE", null, report.getStatus(), "创建Bug简报");
        return report.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BugReportUpdateInput input, Long currentUserId) {
        BugReportPO report = getReportById(id);
        if (!isEditableStatus(report.getStatus())) {
            throw BusinessException.of(ErrorCode.BUG_REPORT_STATUS_INVALID, "仅待填写/已退回状态可编辑");
        }

        List<TicketPO> tickets = Collections.emptyList();
        List<Long> ticketIds = null;
        if (!CollectionUtils.isEmpty(input.getTicketIds())) {
            ticketIds = distinctIds(input.getTicketIds());
            tickets = fetchAndValidateTickets(ticketIds);
        }

        applyUpdateInput(report, input);
        if (Boolean.TRUE.equals(input.getAutoPrefill()) && ticketIds != null) {
            applyAutoPrefill(report, tickets, ticketIds);
        }
        bugReportMapper.updateById(report);

        if (ticketIds != null) {
            syncReportTickets(id, ticketIds, 0);
        }
        if (input.getResponsibleUserIds() != null) {
            syncResponsibleUsers(id, distinctIds(input.getResponsibleUserIds()));
        }
        recordLog(id, currentUserId, "EDIT", report.getStatus(), report.getStatus(), "编辑Bug简报");
    }

    /**
     * P0/P1 严重级别需要审核流程（DRAFT → PENDING_REVIEW），
     * P2 及以下直接归档（DRAFT → ARCHIVED），无需审核人。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id, BugReportSubmitInput input, Long currentUserId) {
        BugReportPO report = getReportById(id);
        if (!(BugReportStatus.DRAFT.getCode().equals(report.getStatus())
                || BugReportStatus.REJECTED.getCode().equals(report.getStatus()))) {
            throw BusinessException.of(ErrorCode.BUG_REPORT_STATUS_INVALID, "当前状态不允许提交");
        }
        if (input != null && input.getReviewerId() != null) {
            report.setReviewerId(input.getReviewerId());
        }

        boolean requiresReview = isHighSeverity(report.getSeverityLevel());
        String oldStatus = report.getStatus();
        String remark = input != null ? input.getRemark() : null;
        report.setSubmittedAt(new Date());

        if (requiresReview) {
            if (report.getReviewerId() == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "P0/P1级别简报提交前请指定审核人");
            }
            report.setStatus(BugReportStatus.PENDING_REVIEW.getCode());
            bugReportMapper.updateById(report);
            recordLog(id, currentUserId, "SUBMIT", oldStatus, report.getStatus(), remark);

            String title = String.format("Bug简报待审核 - %s", report.getReportNo());
            String content = String.format("Bug简报 %s 已提交审核，请及时处理", report.getReportNo());
            notificationOrchestrator.dispatch(report.getReviewerId(), null, report.getId(),
                    NotificationType.REPORT_SUBMITTED, title, content);
        } else {
            // P2及以下直接归档，无需审核
            report.setStatus(BugReportStatus.ARCHIVED.getCode());
            report.setReviewedAt(new Date());
            bugReportMapper.updateById(report);
            recordLog(id, currentUserId, "SUBMIT_ARCHIVED", oldStatus, report.getStatus(), remark);

            // 通知简报责任人已归档
            List<Long> responsibleUserIds = findResponsibleUserIds(id);
            if (!responsibleUserIds.isEmpty()) {
                String severity = StringUtils.hasText(report.getSeverityLevel()) ? report.getSeverityLevel() : "-";
                String notifyTitle = String.format("Bug简报已归档 - %s", report.getReportNo());
                String notifyContent = String.format("Bug简报 %s 已提交并直接归档（%s级别无需审核）",
                        report.getReportNo(), severity);
                notificationOrchestrator.dispatchToUsers(responsibleUserIds, null, report.getId(),
                        NotificationType.REPORT_APPROVED, notifyTitle, notifyContent);
            }
        }
    }

    /**
     * 判断是否属于高严重级别（P0/P1），需要走审核流程。
     * P2 及以下（P2、P3、P4）提交后直接归档。
     */
    private boolean isHighSeverity(String severityLevel) {
        if (!StringUtils.hasText(severityLevel)) {
            return false;
        }
        String code = severityLevel.trim().toUpperCase(Locale.ROOT);
        return "P0".equals(code) || "P1".equals(code);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, BugReportApproveInput input, Long currentUserId) {
        BugReportPO report = getReportById(id);
        if (!BugReportStatus.PENDING_REVIEW.getCode().equals(report.getStatus())) {
            throw BusinessException.of(ErrorCode.BUG_REPORT_STATUS_INVALID, "当前状态不允许审核通过");
        }
        String oldStatus = report.getStatus();
        report.setStatus(BugReportStatus.ARCHIVED.getCode());
        report.setReviewedAt(new Date());
        String reviewComment = input != null ? input.getReviewComment() : null;
        report.setReviewComment(reviewComment);
        bugReportMapper.updateById(report);
        recordLog(id, currentUserId, "APPROVE", oldStatus, report.getStatus(), reviewComment);

        String notifyTitle = String.format("Bug简报已归档 - %s", report.getReportNo());
        String severity = StringUtils.hasText(report.getSeverityLevel()) ? report.getSeverityLevel() : "-";
        String category = StringUtils.hasText(report.getDefectCategory()) ? report.getDefectCategory() : "-";
        // 通知简报责任人（站内信 + 企微应用消息）
        List<Long> responsibleUserIds = findResponsibleUserIds(id);
        if (!responsibleUserIds.isEmpty()) {
            String content = String.format("Bug简报 %s 已审核通过并归档，缺陷分类：%s，严重级别：%s",
                    report.getReportNo(), category, severity);
            notificationOrchestrator.dispatchToUsers(responsibleUserIds, null, report.getId(),
                    NotificationType.REPORT_APPROVED, notifyTitle, content);
        }

        // 查找关联工单及其创建人、处理人
        List<BugReportTicketPO> reportTicketLinks = bugReportTicketMapper.selectList(
                new LambdaQueryWrapper<BugReportTicketPO>()
                        .eq(BugReportTicketPO::getReportId, id));
        if (!CollectionUtils.isEmpty(reportTicketLinks)) {
            List<Long> relatedTicketIds = reportTicketLinks.stream()
                    .map(BugReportTicketPO::getTicketId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<TicketPO> relatedTickets = ticketMapper.selectBatchIds(relatedTicketIds);
            Set<Long> ticketStakeholderIds = new LinkedHashSet<>();
            for (TicketPO ticket : relatedTickets) {
                if (ticket.getCreatorId() != null) {
                    ticketStakeholderIds.add(ticket.getCreatorId());
                }
                if (ticket.getAssigneeId() != null) {
                    ticketStakeholderIds.add(ticket.getAssigneeId());
                }
            }

            // 去掉已在简报责任人中通知过的用户，避免重复
            ticketStakeholderIds.removeAll(responsibleUserIds);

            // 通知工单创建人和处理人（站内信 + 企微应用消息）
            if (!ticketStakeholderIds.isEmpty()) {
                String content = String.format("你参与的工单关联的Bug简报 %s 已审核通过并归档，缺陷分类：%s，严重级别：%s",
                        report.getReportNo(), category, severity);
                notificationOrchestrator.dispatchToUsers(new ArrayList<>(ticketStakeholderIds), null, report.getId(),
                        NotificationType.REPORT_APPROVED, notifyTitle, content);
            }

            // 企微群 Webhook @mention 通知（所有工单相关人员）
            Set<Long> allMentionUserIds = new LinkedHashSet<>(ticketStakeholderIds);
            allMentionUserIds.addAll(responsibleUserIds);
            if (!relatedTicketIds.isEmpty()) {
                // 批量查用户名：reporter + reviewer + 责任人 + 待@人
                Set<Long> allUserIds = new LinkedHashSet<>(allMentionUserIds);
                if (report.getReporterId() != null) {
                    allUserIds.add(report.getReporterId());
                }
                if (report.getReviewerId() != null) {
                    allUserIds.add(report.getReviewerId());
                }
                allUserIds.addAll(responsibleUserIds);
                Map<Long, String> userNameMap = getUserNameMap(allUserIds);

                List<String> mentionWecomUserIds = Collections.emptyList();
                if (!allMentionUserIds.isEmpty()) {
                    List<SysUserPO> mentionUsers = sysUserMapper.selectBatchIds(new ArrayList<>(allMentionUserIds));
                    mentionWecomUserIds = mentionUsers.stream()
                            .map(SysUserPO::getWecomUserid)
                            .filter(StringUtils::hasText)
                            .collect(Collectors.toList());
                }

                String markdownBody = buildApproveGroupNoticeMarkdown(report, userNameMap, responsibleUserIds);
                wecomGroupPushService.pushReportNoticeByTickets(relatedTicketIds, markdownBody, mentionWecomUserIds);
            }
        }
    }

    /**
     * 按照团队既有格式组装 Bug 简报归档群通知 Markdown 正文
     * 格式参考：问题描述：xxx\n逻辑归因：xxx\n缺陷分类：xxx\n...
     */
    private String buildApproveGroupNoticeMarkdown(BugReportPO report,
                                                    Map<Long, String> userNameMap,
                                                    List<Long> responsibleUserIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("**[Bug简报归档] ").append(report.getReportNo()).append("**\n");

        appendNoticeField(sb, "问题描述", report.getProblemDesc());
        appendNoticeField(sb, "逻辑归因", buildLogicCauseText(report));
        appendNoticeField(sb, "缺陷分类", report.getDefectCategory());
        appendNoticeField(sb, "引入项目", report.getIntroducedProject());
        appendNoticeField(sb, "开始时间", formatDateOnly(report.getStartDate()));
        appendNoticeField(sb, "解决时间", formatDateOnly(report.getResolveDate()));
        appendNoticeField(sb, "解决方案", report.getSolution());
        appendNoticeField(sb, "影响范围", report.getImpactScope());
        appendNoticeField(sb, "缺陷等级", report.getSeverityLevel());
        appendNoticeField(sb, "反馈人", userNameMap.get(report.getReporterId()));
        appendNoticeField(sb, "审核人", userNameMap.get(report.getReviewerId()));

        if (!CollectionUtils.isEmpty(responsibleUserIds)) {
            String responsibleNames = responsibleUserIds.stream()
                    .map(userNameMap::get)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("、"));
            appendNoticeField(sb, "责任人", responsibleNames);
        }

        return sb.toString().trim();
    }

    private void appendNoticeField(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(label).append("：").append(value.trim()).append("\n");
        }
    }

    private String buildLogicCauseText(BugReportPO report) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(report.getLogicCauseLevel1())) {
            sb.append(report.getLogicCauseLevel1().trim());
            if (StringUtils.hasText(report.getLogicCauseLevel2())) {
                sb.append("-").append(report.getLogicCauseLevel2().trim());
            }
        } else if (StringUtils.hasText(report.getLogicCauseLevel2())) {
            sb.append(report.getLogicCauseLevel2().trim());
        }
        if (StringUtils.hasText(report.getLogicCauseDetail())) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(report.getLogicCauseDetail().trim());
        }
        return sb.toString();
    }

    private String formatDateOnly(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, BugReportReviewInput input, Long currentUserId) {
        BugReportPO report = getReportById(id);
        if (!BugReportStatus.PENDING_REVIEW.getCode().equals(report.getStatus())) {
            throw BusinessException.of(ErrorCode.BUG_REPORT_STATUS_INVALID, "当前状态不允许审核驳回");
        }
        String oldStatus = report.getStatus();
        report.setStatus(BugReportStatus.REJECTED.getCode());
        report.setReviewedAt(new Date());
        report.setReviewComment(input.getReviewComment());
        bugReportMapper.updateById(report);
        recordLog(id, currentUserId, "REJECT", oldStatus, report.getStatus(), input.getReviewComment());

        List<Long> responsibleUserIds = findResponsibleUserIds(id);
        if (!responsibleUserIds.isEmpty()) {
            String title = String.format("Bug简报审核驳回 - %s", report.getReportNo());
            String content = String.format("Bug简报 %s 审核未通过，请根据意见修改后重新提交", report.getReportNo());
            notificationOrchestrator.dispatchToUsers(responsibleUserIds, null, report.getId(),
                    NotificationType.REPORT_REJECTED, title, content);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidReport(Long id, Long currentUserId) {
        BugReportPO report = getReportById(id);
        if (BugReportStatus.VOIDED.getCode().equals(report.getStatus())) {
            return;
        }
        if (BugReportStatus.ARCHIVED.getCode().equals(report.getStatus())) {
            throw BusinessException.of(ErrorCode.BUG_REPORT_STATUS_INVALID, "已归档简报不允许作废");
        }
        String oldStatus = report.getStatus();
        report.setStatus(BugReportStatus.VOIDED.getCode());
        bugReportMapper.updateById(report);
        recordLog(id, currentUserId, "VOID", oldStatus, report.getStatus(), "作废Bug简报");
    }

    public BugReportStatisticsOutput statistics(BugReportStatisticsInput input) {
        Date createStart = parseDateTime(input != null ? input.getCreateTimeStart() : null, false);
        Date createEnd = parseDateTime(input != null ? input.getCreateTimeEnd() : null, true);

        LambdaQueryWrapper<BugReportPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(BugReportPO::getStatus, BugReportStatus.VOIDED.getCode());
        if (createStart != null) {
            wrapper.ge(BugReportPO::getCreateTime, createStart);
        }
        if (createEnd != null) {
            wrapper.le(BugReportPO::getCreateTime, createEnd);
        }
        List<BugReportPO> reports = bugReportMapper.selectList(wrapper);

        BugReportStatisticsOutput output = new BugReportStatisticsOutput();
        if (CollectionUtils.isEmpty(reports)) {
            output.setLogicCauseDistribution(Collections.emptyList());
            output.setDefectCategoryDistribution(Collections.emptyList());
            output.setIntroducedProjectTop(Collections.emptyList());
            output.setResponsibleStatistics(Collections.emptyList());
            output.setTimelyCount(0);
            output.setTotalCount(0);
            output.setTimelyRate(0D);
            return output;
        }

        output.setLogicCauseDistribution(toDistribution(
                reports.stream().map(this::buildLogicCauseKey).collect(Collectors.toList()), 0));
        output.setDefectCategoryDistribution(toDistribution(
                reports.stream().map(BugReportPO::getDefectCategory).collect(Collectors.toList()), 0));
        output.setIntroducedProjectTop(toDistribution(
                reports.stream().map(BugReportPO::getIntroducedProject).collect(Collectors.toList()), 10));
        output.setResponsibleStatistics(buildResponsibleStatistics(reports));

        int remindDays = getBugReportRemindDays();
        int timelyCount = 0;
        for (BugReportPO report : reports) {
            if (report.getCreateTime() == null || report.getSubmittedAt() == null) {
                continue;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(report.getCreateTime());
            calendar.add(Calendar.DAY_OF_MONTH, remindDays);
            if (!report.getSubmittedAt().after(calendar.getTime())) {
                timelyCount++;
            }
        }
        int totalCount = reports.size();
        output.setTimelyCount(timelyCount);
        output.setTotalCount(totalCount);
        output.setTimelyRate(totalCount == 0 ? 0D : round((timelyCount * 100D) / totalCount, 2));
        return output;
    }

    /**
     * 工单进入终态后自动创建简报草稿（无已有关联时）。
     * 注意：终态为「已关闭」时由 {@link BugReportEventListener} 跳过，不调用本方法。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDraftFromClosedTicket(Long ticketId, Long operatorId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return null;
        }
        if (!isBugTicket(ticketId)) {
            return null;
        }

        LambdaQueryWrapper<BugReportTicketPO> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(BugReportTicketPO::getTicketId, ticketId);
        List<BugReportTicketPO> existingLinks = bugReportTicketMapper.selectList(existingWrapper);
        if (!CollectionUtils.isEmpty(existingLinks)) {
            return existingLinks.get(0).getReportId();
        }

        BugReportPO report = new BugReportPO();
        report.setReportNo(TicketNoGenerator.generateBugReportNo());
        report.setStatus(BugReportStatus.DRAFT.getCode());
        report.setCreatedByUserId(operatorId != null ? operatorId : ticket.getCreatorId());
        report.setReporterId(ticket.getCreatorId());
        report.setRemark("系统自动创建");
        applyAutoPrefill(report, Collections.singletonList(ticket), Collections.singletonList(ticketId));
        bugReportMapper.insert(report);

        syncReportTickets(report.getId(), Collections.singletonList(ticketId), 1);
        List<Long> responsibleIds = new ArrayList<>();
        if (ticket.getAssigneeId() != null) {
            responsibleIds.add(ticket.getAssigneeId());
        }
        syncResponsibleUsers(report.getId(), responsibleIds);
        recordLog(report.getId(), operatorId, "CREATE", null, report.getStatus(), "工单关闭自动创建简报草稿");

        if (!responsibleIds.isEmpty()) {
            String title = String.format("Bug简报草稿已生成 - %s", report.getReportNo());
            String content = String.format("工单 %s 关闭后已自动生成Bug简报草稿，请尽快完善并提交审核", ticket.getTicketNo());
            notificationOrchestrator.dispatchToUsers(responsibleIds, ticketId, report.getId(),
                    NotificationType.REPORT_REMIND, title, content);
        }
        return report.getId();
    }

    /**
     * 超期提醒
     */
    public void remindOverdueReports() {
        int remindDays = getBugReportRemindDays();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -remindDays);
        Date threshold = calendar.getTime();

        List<BugReportPO> overdueReports = bugReportMapper.selectList(
                new LambdaQueryWrapper<BugReportPO>()
                        .in(BugReportPO::getStatus,
                                BugReportStatus.DRAFT.getCode(),
                                BugReportStatus.REJECTED.getCode())
                        .le(BugReportPO::getCreateTime, threshold)
        );
        if (CollectionUtils.isEmpty(overdueReports)) {
            return;
        }

        List<Long> reportIds = overdueReports.stream().map(BugReportPO::getId).collect(Collectors.toList());
        List<BugReportResponsiblePO> responsibleList = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>()
                        .in(BugReportResponsiblePO::getReportId, reportIds)
        );
        Map<Long, List<Long>> responsibleMap = responsibleList.stream()
                .collect(Collectors.groupingBy(BugReportResponsiblePO::getReportId,
                        Collectors.mapping(BugReportResponsiblePO::getUserId, Collectors.toList())));

        for (BugReportPO report : overdueReports) {
            List<Long> userIds = responsibleMap.get(report.getId());
            if (CollectionUtils.isEmpty(userIds)) {
                continue;
            }
            String title = String.format("Bug简报超期提醒 - %s", report.getReportNo());
            String content = String.format("Bug简报 %s 已超过 %d 天未完成，请尽快处理",
                    report.getReportNo(), remindDays);
            notificationOrchestrator.dispatchToUsers(userIds, null, report.getId(),
                    NotificationType.REPORT_REMIND, title, content);
        }
    }

    private boolean isEditableStatus(String status) {
        return BugReportStatus.DRAFT.getCode().equals(status)
                || BugReportStatus.REJECTED.getCode().equals(status);
    }

    private BugReportPO getReportById(Long id) {
        BugReportPO report = bugReportMapper.selectById(id);
        if (report == null) {
            throw BusinessException.of(ErrorCode.BUG_REPORT_NOT_FOUND);
        }
        return report;
    }

    private void applyCreateInput(BugReportPO report, BugReportCreateInput input) {
        report.setProblemDesc(input.getProblemDesc());
        report.setLogicCauseLevel1(input.getLogicCauseLevel1());
        report.setLogicCauseLevel2(input.getLogicCauseLevel2());
        report.setLogicCauseDetail(input.getLogicCauseDetail());
        report.setDefectCategory(input.getDefectCategory());
        report.setIntroducedProject(input.getIntroducedProject());
        report.setStartDate(input.getStartDate());
        report.setResolveDate(input.getResolveDate());
        report.setTempResolveDate(input.getTempResolveDate());
        report.setSolution(input.getSolution());
        report.setTempSolution(input.getTempSolution());
        report.setImpactScope(input.getImpactScope());
        report.setSeverityLevel(input.getSeverityLevel());
        report.setReporterId(input.getReporterId());
        report.setReviewerId(input.getReviewerId());
        report.setRemark(input.getRemark());
    }

    private void applyUpdateInput(BugReportPO report, BugReportUpdateInput input) {
        if (input.getProblemDesc() != null) {
            report.setProblemDesc(input.getProblemDesc());
        }
        if (input.getLogicCauseLevel1() != null) {
            report.setLogicCauseLevel1(input.getLogicCauseLevel1());
        }
        if (input.getLogicCauseLevel2() != null) {
            report.setLogicCauseLevel2(input.getLogicCauseLevel2());
        }
        if (input.getLogicCauseDetail() != null) {
            report.setLogicCauseDetail(input.getLogicCauseDetail());
        }
        if (input.getDefectCategory() != null) {
            report.setDefectCategory(input.getDefectCategory());
        }
        if (input.getIntroducedProject() != null) {
            report.setIntroducedProject(input.getIntroducedProject());
        }
        if (input.getStartDate() != null) {
            report.setStartDate(input.getStartDate());
        }
        if (input.getResolveDate() != null) {
            report.setResolveDate(input.getResolveDate());
        }
        if (input.getTempResolveDate() != null) {
            report.setTempResolveDate(input.getTempResolveDate());
        }
        if (input.getSolution() != null) {
            report.setSolution(input.getSolution());
        }
        if (input.getTempSolution() != null) {
            report.setTempSolution(input.getTempSolution());
        }
        if (input.getImpactScope() != null) {
            report.setImpactScope(input.getImpactScope());
        }
        if (input.getSeverityLevel() != null) {
            report.setSeverityLevel(input.getSeverityLevel());
        }
        if (input.getReporterId() != null) {
            report.setReporterId(input.getReporterId());
        }
        if (input.getReviewerId() != null) {
            report.setReviewerId(input.getReviewerId());
        }
        if (input.getRemark() != null) {
            report.setRemark(input.getRemark());
        }
    }

    private List<TicketPO> fetchAndValidateTickets(List<Long> ticketIds) {
        if (CollectionUtils.isEmpty(ticketIds)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "关联工单不能为空");
        }
        List<TicketPO> tickets = ticketMapper.selectBatchIds(ticketIds);
        if (CollectionUtils.isEmpty(tickets) || tickets.size() != ticketIds.size()) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND, "存在无效工单ID");
        }
        return tickets;
    }

    private void applyAutoPrefill(BugReportPO report, List<TicketPO> tickets, List<Long> ticketIds) {
        if (CollectionUtils.isEmpty(tickets) || CollectionUtils.isEmpty(ticketIds)) {
            return;
        }

        Map<Long, TicketBugInfoPO> bugInfoMap = ticketBugInfoMapper.selectList(
                        new LambdaQueryWrapper<TicketBugInfoPO>().in(TicketBugInfoPO::getTicketId, ticketIds))
                .stream().collect(Collectors.toMap(TicketBugInfoPO::getTicketId, i -> i, (a, b) -> a));
        Map<Long, TicketBugTestInfoPO> bugTestInfoMap = ticketBugTestInfoMapper.selectList(
                        new LambdaQueryWrapper<TicketBugTestInfoPO>().in(TicketBugTestInfoPO::getTicketId, ticketIds))
                .stream().collect(Collectors.toMap(TicketBugTestInfoPO::getTicketId, i -> i, (a, b) -> a));
        Map<Long, TicketBugDevInfoPO> bugDevInfoMap = ticketBugDevInfoMapper.selectList(
                        new LambdaQueryWrapper<TicketBugDevInfoPO>().in(TicketBugDevInfoPO::getTicketId, ticketIds))
                .stream().collect(Collectors.toMap(TicketBugDevInfoPO::getTicketId, i -> i, (a, b) -> a));

        if (!StringUtils.hasText(report.getProblemDesc())) {
            for (TicketPO ticket : tickets) {
                TicketBugInfoPO infoPO = bugInfoMap.get(ticket.getId());
                String problemDesc = infoPO != null ? infoPO.getProblemDesc() : null;
                if (!StringUtils.hasText(problemDesc)) {
                    problemDesc = ticket.getDescription();
                }
                if (StringUtils.hasText(problemDesc)) {
                    report.setProblemDesc(problemDesc);
                    break;
                }
            }
        }

        if (report.getStartDate() == null) {
            Date startDate = tickets.stream()
                    .map(TicketPO::getCreateTime)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);
            report.setStartDate(startDate);
        }

        if (report.getResolveDate() == null) {
            Date resolveDate = null;
            for (TicketPO ticket : tickets) {
                Date candidate = ticket.getResolvedAt() != null ? ticket.getResolvedAt() : ticket.getClosedAt();
                if (candidate != null && (resolveDate == null || candidate.after(resolveDate))) {
                    resolveDate = candidate;
                }
            }
            report.setResolveDate(resolveDate);
        }

        if (report.getReporterId() == null) {
            for (TicketPO ticket : tickets) {
                if (ticket.getCreatorId() != null) {
                    report.setReporterId(ticket.getCreatorId());
                    break;
                }
            }
        }

        if (!StringUtils.hasText(report.getSeverityLevel())) {
            for (TicketPO ticket : tickets) {
                TicketBugTestInfoPO testInfoPO = bugTestInfoMap.get(ticket.getId());
                if (testInfoPO != null && StringUtils.hasText(testInfoPO.getSeverityLevel())) {
                    report.setSeverityLevel(normalizeSeverity(testInfoPO.getSeverityLevel()));
                    break;
                }
            }
        }

        if (!StringUtils.hasText(report.getSolution())) {
            for (TicketPO ticket : tickets) {
                TicketBugDevInfoPO devInfoPO = bugDevInfoMap.get(ticket.getId());
                if (devInfoPO != null && StringUtils.hasText(devInfoPO.getFixSolution())) {
                    report.setSolution(devInfoPO.getFixSolution());
                    break;
                }
            }
        }

        if (!StringUtils.hasText(report.getImpactScope())) {
            for (TicketPO ticket : tickets) {
                TicketBugTestInfoPO testInfoPO = bugTestInfoMap.get(ticket.getId());
                if (testInfoPO != null && StringUtils.hasText(testInfoPO.getImpactScope())) {
                    report.setImpactScope(testInfoPO.getImpactScope());
                    break;
                }
                TicketBugDevInfoPO devInfoPO = bugDevInfoMap.get(ticket.getId());
                if (devInfoPO != null && StringUtils.hasText(devInfoPO.getImpactAssessment())) {
                    report.setImpactScope(devInfoPO.getImpactAssessment());
                    break;
                }
            }
        }

        if (!StringUtils.hasText(report.getIntroducedProject())) {
            for (TicketPO ticket : tickets) {
                TicketBugTestInfoPO testInfoPO = bugTestInfoMap.get(ticket.getId());
                if (testInfoPO != null && StringUtils.hasText(testInfoPO.getModuleName())) {
                    report.setIntroducedProject(testInfoPO.getModuleName());
                    break;
                }
            }
        }
    }

    private String normalizeSeverity(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String value = source.trim().toUpperCase(Locale.ROOT);
        if ("FATAL".equals(value)) {
            return "P0";
        }
        if ("CRITICAL".equals(value)) {
            return "P1";
        }
        if ("NORMAL".equals(value)) {
            return "P2";
        }
        if ("MINOR".equals(value)) {
            return "P3";
        }
        return source;
    }

    private void syncReportTickets(Long reportId, List<Long> ticketIds, Integer isAutoCreated) {
        bugReportTicketMapper.hardDeleteByReportId(reportId);
        if (CollectionUtils.isEmpty(ticketIds)) {
            return;
        }
        int autoCreatedVal = isAutoCreated == null ? 0 : isAutoCreated;
        List<BugReportTicketPO> batchList = ticketIds.stream()
                .filter(Objects::nonNull)
                .map(ticketId -> {
                    BugReportTicketPO relation = new BugReportTicketPO();
                    relation.setReportId(reportId);
                    relation.setTicketId(ticketId);
                    relation.setIsAutoCreated(autoCreatedVal);
                    return relation;
                })
                .collect(Collectors.toList());
        if (!batchList.isEmpty()) {
            bugReportTicketMapper.batchInsert(batchList);
        }
    }

    private void syncResponsibleUsers(Long reportId, List<Long> responsibleUserIds) {
        bugReportResponsibleMapper.hardDeleteByReportId(reportId);
        if (CollectionUtils.isEmpty(responsibleUserIds)) {
            return;
        }
        List<BugReportResponsiblePO> batchList = responsibleUserIds.stream()
                .filter(Objects::nonNull)
                .map(userId -> {
                    BugReportResponsiblePO relation = new BugReportResponsiblePO();
                    relation.setReportId(reportId);
                    relation.setUserId(userId);
                    return relation;
                })
                .collect(Collectors.toList());
        if (!batchList.isEmpty()) {
            bugReportResponsibleMapper.batchInsert(batchList);
        }
    }

    private void recordLog(Long reportId, Long userId, String action,
                           String oldStatus, String newStatus, String remark) {
        BugReportLogPO logPO = new BugReportLogPO();
        logPO.setReportId(reportId);
        logPO.setUserId(userId != null ? userId : 0L);
        logPO.setAction(action);
        logPO.setOldStatus(oldStatus);
        logPO.setNewStatus(newStatus);
        logPO.setRemark(remark);
        bugReportLogMapper.insert(logPO);
    }

    private Map<Long, String> getUserNameMap(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName, (a, b) -> a));
    }

    private BugReportPageOutput convertToPageOutput(BugReportPO item, Map<Long, String> reviewerNameMap) {
        BugReportPageOutput output = new BugReportPageOutput();
        output.setId(item.getId());
        output.setReportNo(item.getReportNo());
        output.setStatus(item.getStatus());
        BugReportStatus status = BugReportStatus.fromCode(item.getStatus());
        output.setStatusLabel(status != null ? status.getLabel() : item.getStatus());
        output.setDefectCategory(item.getDefectCategory());
        output.setSeverityLevel(item.getSeverityLevel());
        output.setReviewerId(item.getReviewerId());
        output.setReviewerName(reviewerNameMap.get(item.getReviewerId()));
        output.setSubmittedAt(item.getSubmittedAt());
        output.setCreateTime(item.getCreateTime());
        output.setUpdateTime(item.getUpdateTime());
        return output;
    }

    private Set<Long> findReportIdsByResponsible(Long userId) {
        List<BugReportResponsiblePO> links = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>().eq(BugReportResponsiblePO::getUserId, userId)
        );
        return links.stream().map(BugReportResponsiblePO::getReportId).collect(Collectors.toSet());
    }

    private Set<Long> findReportIdsByTicket(Long ticketId) {
        List<BugReportTicketPO> links = bugReportTicketMapper.selectList(
                new LambdaQueryWrapper<BugReportTicketPO>().eq(BugReportTicketPO::getTicketId, ticketId)
        );
        return links.stream().map(BugReportTicketPO::getReportId).collect(Collectors.toSet());
    }

    private List<Long> findResponsibleUserIds(Long reportId) {
        List<BugReportResponsiblePO> links = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>().eq(BugReportResponsiblePO::getReportId, reportId)
        );
        return links.stream().map(BugReportResponsiblePO::getUserId).distinct().collect(Collectors.toList());
    }

    private List<Long> mergeResponsibleUserIds(List<Long> inputUserIds, List<TicketPO> tickets) {
        LinkedHashSet<Long> merged = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(inputUserIds)) {
            merged.addAll(inputUserIds.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }
        for (TicketPO ticket : tickets) {
            if (ticket.getAssigneeId() != null) {
                merged.add(ticket.getAssigneeId());
            }
        }
        return new ArrayList<>(merged);
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<BugReportStatisticsOutput.DistributionItem> toDistribution(List<String> names, int limit) {
        Map<String, Long> grouped = names.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(String::trim, Collectors.counting()));
        long total = grouped.values().stream().mapToLong(Long::longValue).sum();
        List<BugReportStatisticsOutput.DistributionItem> result = grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> {
                    BugReportStatisticsOutput.DistributionItem item = new BugReportStatisticsOutput.DistributionItem();
                    item.setName(entry.getKey());
                    item.setCount(entry.getValue());
                    double rate = total > 0 ? Math.round(entry.getValue() * 1000.0 / total) / 10.0 : 0.0;
                    item.setRate(rate);
                    return item;
                }).collect(Collectors.toList());
        if (limit > 0 && result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    private List<BugReportStatisticsOutput.ResponsibleStatItem> buildResponsibleStatistics(List<BugReportPO> reports) {
        List<Long> reportIds = reports.stream().map(BugReportPO::getId).collect(Collectors.toList());
        List<BugReportResponsiblePO> responsibleList = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>().in(BugReportResponsiblePO::getReportId, reportIds)
        );
        if (CollectionUtils.isEmpty(responsibleList)) {
            return Collections.emptyList();
        }

        Map<Long, Long> userCountMap = responsibleList.stream()
                .collect(Collectors.groupingBy(BugReportResponsiblePO::getUserId, Collectors.counting()));
        Map<Long, String> userNameMap = getUserNameMap(userCountMap.keySet());

        return userCountMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> {
                    BugReportStatisticsOutput.ResponsibleStatItem item =
                            new BugReportStatisticsOutput.ResponsibleStatItem();
                    item.setUserId(entry.getKey());
                    item.setUserName(userNameMap.get(entry.getKey()));
                    item.setCount(entry.getValue());
                    return item;
                }).collect(Collectors.toList());
    }

    private String buildLogicCauseKey(BugReportPO report) {
        boolean hasLevel1 = StringUtils.hasText(report.getLogicCauseLevel1());
        boolean hasLevel2 = StringUtils.hasText(report.getLogicCauseLevel2());
        if (hasLevel1 && hasLevel2) {
            return report.getLogicCauseLevel1().trim() + " / " + report.getLogicCauseLevel2().trim();
        }
        if (hasLevel1) {
            return report.getLogicCauseLevel1().trim();
        }
        if (hasLevel2) {
            return report.getLogicCauseLevel2().trim();
        }
        return null;
    }

    private Date parseDateTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() <= 10) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
                Date date = sdf.parse(text);
                if (date == null) {
                    return null;
                }
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                if (endOfDay) {
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                }
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTime();
            }
            SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_PATTERN);
            return sdf.parse(text);
        } catch (ParseException e) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "时间格式错误，需为yyyy-MM-dd或yyyy-MM-dd HH:mm:ss");
        }
    }

    private int getBugReportRemindDays() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, "bug_report_remind_days");
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return 3;
        }
        try {
            int value = Integer.parseInt(config.getConfigValue().trim());
            return value > 0 ? value : 3;
        } catch (NumberFormatException ex) {
            return 3;
        }
    }

    private boolean isBugTicket(Long ticketId) {
        long csCount = ticketBugInfoMapper.selectCount(
                new LambdaQueryWrapper<TicketBugInfoPO>().eq(TicketBugInfoPO::getTicketId, ticketId));
        if (csCount > 0) {
            return true;
        }
        long testCount = ticketBugTestInfoMapper.selectCount(
                new LambdaQueryWrapper<TicketBugTestInfoPO>().eq(TicketBugTestInfoPO::getTicketId, ticketId));
        if (testCount > 0) {
            return true;
        }
        long devCount = ticketBugDevInfoMapper.selectCount(
                new LambdaQueryWrapper<TicketBugDevInfoPO>().eq(TicketBugDevInfoPO::getTicketId, ticketId));
        return devCount > 0;
    }

    private double round(double value, int scale) {
        double base = Math.pow(10, scale);
        return Math.round(value * base) / base;
    }
}

package com.miduo.cloud.ticket.application.ticket;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.workflow.TicketWorkflowAppService;
import com.miduo.cloud.ticket.common.constants.AppConstants;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.*;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.util.TicketNoGenerator;
import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketAutoDispatchEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCommentMentionEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.domain.common.event.DomainEvent;
import com.miduo.cloud.ticket.application.workflow.WorkflowApplicationService;
import com.miduo.cloud.ticket.entity.dto.ticket.*;
import com.miduo.cloud.ticket.entity.dto.workflow.TransitInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportResponsibleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportTicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportResponsiblePO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TicketApplicationService {

    private static final Logger log = LoggerFactory.getLogger(TicketApplicationService.class);

    /** 内置缺陷工单工作流（与 Flyway 初始化 id=3 一致） */
    private static final long DEFECT_WORKFLOW_ID = 3L;

    private static final long ALERT_WORKFLOW_ID = 4L;

    private static final int COMMENT_MENTION_SUMMARY_MAX = 200;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /** 评论正文中 @ 占位（与前端 data-user-id 一致），用于在请求体未带 mentionedUserIds 时仍能识别被 @ 用户 */
    private static final Pattern COMMENT_MENTION_DATA_USER_ID =
            Pattern.compile("(?i)data-user-id\\s*=\\s*[\"']?(\\d+)[\"']?");

    /**
     * 正文中的 @姓名(用户ID)（前端在富文本清洗掉 data-user-id 时的兜底，ID 必须为纯数字）
     */
    private static final Pattern COMMENT_MENTION_NAME_PAREN_ID =
            Pattern.compile("@([^(\\s<]+)\\((\\d{1,19})\\)");

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
    private BugReportResponsibleMapper bugReportResponsibleMapper;

    @Resource
    private TicketTimeTrackApplicationService ticketTimeTrackService;

    @Resource
    private TicketBugApplicationService ticketBugApplicationService;

    @Resource
    private TicketWorkflowAppService ticketWorkflowAppService;

    @Resource
    private com.miduo.cloud.ticket.application.sla.SlaTimerService slaTimerService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private TicketAssigneeSyncService ticketAssigneeSyncService;

    @Resource
    private TicketBugInfoMapper ticketBugInfoMapper;

    @Resource
    private TicketAssigneeMapper ticketAssigneeMapper;

    @Resource
    private com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.mapper.SlaTimerMapper slaTimerMapper;

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

        if (ticket.getAssigneeId() != null) {
            ticketAssigneeSyncService.applyAssigneesToTicket(
                    ticket, Collections.singletonList(ticket.getAssigneeId()));
        }

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
        safeRecordCreateTrack(ticket, currentUserId);

        boolean pendingAutoDispatch = ticket.getAssigneeId() == null && input.getAssigneeId() == null;
        safePublishEvent(new TicketCreatedEvent(
                ticket.getId(), ticket.getCategoryId(), ticket.getPriority(), pendingAutoDispatch));
        if (ticket.getAssigneeId() != null) {
            safePublishEvent(new TicketAssignedEvent(
                    ticket.getId(), ticket.getAssigneeId(), null, currentUserId,
                    TicketAssignType.CREATE_ASSIGN.getCode()));
        }

        // 启动SLA计时器（若分类绑定了SLA策略）
        if (category.getSlaPolicyId() != null) {
            try {
                slaTimerService.startTimers(ticket.getId(), category.getSlaPolicyId());
            } catch (Exception e) {
                log.warn("SLA计时器启动失败，不影响工单创建: ticketId={}, policyId={}, error={}",
                        ticket.getId(), category.getSlaPolicyId(), e.getMessage());
            }
        }

        // 未指定处理人时：事务提交后再自动分派（TicketAutoDispatchListener，AFTER_COMMIT + fallbackExecution，与 Webhook 一致；避免仅依赖 TransactionSynchronization 在 @Async 企微链路下未触发）
        if (ticket.getAssigneeId() == null && input.getAssigneeId() == null) {
            safePublishEvent(new TicketAutoDispatchEvent(ticket.getId()));
        }

        log.info("工单创建成功: ticketNo={}, creatorId={}", ticket.getTicketNo(), currentUserId);
        return ticket.getId();
    }

    public PageOutput<TicketListOutput> getTicketPage(TicketPageInput input, Long currentUserId) {
        Page<TicketPO> page = new Page<>(input.getPageNum(), input.getPageSize());

        String viewCode = TicketView.fromCode(input.getView()).getCode();

        String keyword = input.getKeyword() == null ? null : input.getKeyword().trim();
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }

        IPage<TicketPO> result = ticketMapper.selectTicketPage(
                page,
                viewCode,
                currentUserId,
                keyword,
                keyword != null ? null : input.getTicketNo(),
                keyword != null ? null : input.getTitle(),
                input.getCategoryId(),
                input.getStatus(),
                input.getPriority(),
                input.getCreatorId(),
                input.getAssigneeId(),
                input.getCreateTimeStart(),
                input.getCreateTimeEnd(),
                input.getOrderBy(),
                input.isAsc(),
                input.getSlaStatus(),
                input.getLinkableForBugReport()
        );

        List<TicketPO> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            return PageOutput.empty(input.getPageNum(), input.getPageSize());
        }

        List<Long> ticketIds = records.stream()
                .map(TicketPO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, List<Long>> assigneeUserIdsByTicketId = loadAssigneeUserIdsByTicketId(ticketIds);

        Set<Long> userIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (TicketPO t : records) {
            if (t.getCreatorId() != null) {
                userIds.add(t.getCreatorId());
            }
            if (t.getAssigneeId() != null) {
                userIds.add(t.getAssigneeId());
            }
            if (t.getCategoryId() != null) {
                categoryIds.add(t.getCategoryId());
            }
        }
        for (List<Long> assigneeList : assigneeUserIdsByTicketId.values()) {
            if (assigneeList != null) {
                for (Long uid : assigneeList) {
                    if (uid != null) {
                        userIds.add(uid);
                    }
                }
            }
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

        Map<Long, String> multiAssigneeNameByTicketId =
                buildMultiAssigneeDisplayNames(assigneeUserIdsByTicketId, userNameMap);
        Map<Long, String> companyNameByTicketId = Collections.emptyMap();
        if (!ticketIds.isEmpty()) {
            List<TicketBugInfoPO> bugInfos = ticketBugInfoMapper.selectList(
                    new LambdaQueryWrapper<TicketBugInfoPO>().in(TicketBugInfoPO::getTicketId, ticketIds));
            companyNameByTicketId = bugInfos.stream()
                    .filter(b -> b.getTicketId() != null && b.getCompanyName() != null
                            && !b.getCompanyName().trim().isEmpty())
                    .collect(Collectors.toMap(TicketBugInfoPO::getTicketId, TicketBugInfoPO::getCompanyName,
                            (a, b) -> a));
        }

        Map<Long, String> slaStatusByTicketId = resolveSlaStatusByTicketId(ticketIds);

        Map<Long, String> finalUserNameMap = userNameMap;
        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        Map<Long, String> finalCompanyNameMap = companyNameByTicketId;
        Map<Long, String> finalMultiAssigneeNameMap = multiAssigneeNameByTicketId;
        List<TicketListOutput> outputs = records.stream()
                .map(po -> convertToListOutput(po, finalUserNameMap, finalCategoryNameMap, finalCompanyNameMap,
                        finalMultiAssigneeNameMap, slaStatusByTicketId))
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

    /**
     * 新增工单评论
     * 接口编号：API000508（请求体可含 mentionedUserIds，@ 提醒在事务提交后异步推送）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long ticketId, TicketCommentInput input, Long currentUserId) {
        if (input == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "请求体不能为空");
        }
        String content = input.getContent();
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        if (content == null || content.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "评论内容不能为空");
        }
        TicketCommentPO comment = new TicketCommentPO();
        comment.setTicketId(ticketId);
        comment.setUserId(currentUserId);
        comment.setContent(content.trim());
        comment.setType(CommentType.COMMENT.getCode());
        commentMapper.insert(comment);

        List<Long> combinedMentions = mergeExplicitAndHtmlMentions(input.getMentionedUserIds(), content);
        List<Long> mentionTargets = resolveCommentMentionTargets(combinedMentions, currentUserId);
        if (!mentionTargets.isEmpty()) {
            String plainSummary = toCommentPlainSummary(content);
            eventPublisher.publishEvent(new TicketCommentMentionEvent(ticketId, mentionTargets,
                    currentUserId, plainSummary));
        }
        return comment.getId();
    }

    private static List<Long> mergeExplicitAndHtmlMentions(List<Long> explicit, String html) {
        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        if (explicit != null) {
            for (Long id : explicit) {
                if (id != null && id > 0) {
                    ordered.add(id);
                }
            }
        }
        for (Long id : extractMentionUserIdsFromCommentHtml(html)) {
            ordered.add(id);
        }
        return new ArrayList<>(ordered);
    }

    private static List<Long> extractMentionUserIdsFromCommentHtml(String html) {
        if (html == null || html.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Matcher m = COMMENT_MENTION_DATA_USER_ID.matcher(html);
        while (m.find()) {
            try {
                long id = Long.parseLong(m.group(1));
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // skip malformed attribute
            }
        }
        Matcher m2 = COMMENT_MENTION_NAME_PAREN_ID.matcher(html);
        while (m2.find()) {
            try {
                long id = Long.parseLong(m2.group(2));
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * 规范化 @ 用户列表：去重、剔除空与当前用户、剔除无效 ID，并限制人数上限
     */
    private List<Long> resolveCommentMentionTargets(List<Long> rawMentionedUserIds, Long currentUserId) {
        if (rawMentionedUserIds == null || rawMentionedUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        for (Long id : rawMentionedUserIds) {
            if (id == null || id <= 0) {
                continue;
            }
            if (currentUserId != null && currentUserId.equals(id)) {
                continue;
            }
            ordered.add(id);
            if (ordered.size() >= AppConstants.MAX_TICKET_COMMENT_MENTIONS) {
                break;
            }
        }
        if (ordered.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUserPO> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUserPO>()
                        .in(SysUserPO::getId, ordered)
                        .eq(SysUserPO::getAccountStatus, 1));
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> activeIds = users.stream()
                .map(SysUserPO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> result = new ArrayList<>();
        for (Long id : ordered) {
            if (activeIds.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static String toCommentPlainSummary(String htmlOrText) {
        if (htmlOrText == null) {
            return "";
        }
        String plain = HTML_TAG_PATTERN.matcher(htmlOrText).replaceAll(" ");
        plain = plain.replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (plain.length() <= COMMENT_MENTION_SUMMARY_MAX) {
            return plain;
        }
        return plain.substring(0, COMMENT_MENTION_SUMMARY_MAX) + "…";
    }

    /**
     * 按工单编号查询公开详情（无需登录，外网可访问）
     */
    public TicketPublicDetailOutput getPublicTicketDetail(String ticketNo) {
        if (ticketNo == null || ticketNo.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "工单编号不能为空");
        }
        TicketPO ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<TicketPO>()
                        .eq(TicketPO::getTicketNo, ticketNo.trim())
                        .last("LIMIT 1")
        );
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return buildPublicDetailOutput(ticket);
    }

    private TicketPublicDetailOutput buildPublicDetailOutput(TicketPO ticket) {
        TicketPublicDetailOutput output = new TicketPublicDetailOutput();
        output.setId(ticket.getId());
        output.setTicketNo(ticket.getTicketNo());
        output.setTitle(ticket.getTitle());
        output.setDescription(ticket.getDescription());
        output.setPriority(ticket.getPriority());
        output.setStatus(ticket.getStatus());
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

        if (ticket.getCategoryId() != null) {
            TicketCategoryPO category = categoryMapper.selectById(ticket.getCategoryId());
            if (category != null) {
                output.setCategoryName(category.getName());
                output.setCategoryFullPath(buildCategoryFullPath(category));
            }
        }

        Set<Long> userIds = new HashSet<>();
        if (ticket.getCreatorId() != null) {
            userIds.add(ticket.getCreatorId());
        }
        if (ticket.getAssigneeId() != null) {
            userIds.add(ticket.getAssigneeId());
        }

        TicketBugCustomerInfoOutput customerInfoOutput = ticketBugApplicationService.getCustomerInfo(ticket.getId());
        if (customerInfoOutput != null) {
            TicketPublicDetailOutput.BugCustomerInfo bugCustomerInfo = new TicketPublicDetailOutput.BugCustomerInfo();
            bugCustomerInfo.setMerchantNo(customerInfoOutput.getMerchantNo());
            bugCustomerInfo.setCompanyName(customerInfoOutput.getCompanyName());
            bugCustomerInfo.setMerchantAccount(customerInfoOutput.getMerchantAccount());
            bugCustomerInfo.setProblemDesc(customerInfoOutput.getProblemDesc());
            bugCustomerInfo.setExpectedResult(customerInfoOutput.getExpectedResult());
            bugCustomerInfo.setSceneCode(customerInfoOutput.getSceneCode());
            bugCustomerInfo.setProblemScreenshot(customerInfoOutput.getProblemScreenshot());
            output.setBugCustomerInfo(bugCustomerInfo);
        }
        output.setArchivedBugReport(buildArchivedBugReportSummary(ticket.getId()));

        List<TicketCommentPO> comments = commentMapper.selectList(
                new LambdaQueryWrapper<TicketCommentPO>()
                        .eq(TicketCommentPO::getTicketId, ticket.getId())
                        .orderByAsc(TicketCommentPO::getCreateTime)
        );
        if (comments != null) {
            for (TicketCommentPO c : comments) {
                if (c.getUserId() != null) {
                    userIds.add(c.getUserId());
                }
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

        if (comments != null) {
            Map<Long, SysUserPO> finalUserMap = userMap;
            output.setComments(comments.stream().map(c -> {
                TicketPublicDetailOutput.CommentOutput co = new TicketPublicDetailOutput.CommentOutput();
                co.setId(c.getId());
                co.setContent(c.getContent());
                co.setType(c.getType());
                co.setCreateTime(c.getCreateTime());
                SysUserPO commentUser = finalUserMap.get(c.getUserId());
                if (commentUser != null) {
                    co.setUserName(commentUser.getName());
                }
                return co;
            }).collect(Collectors.toList()));
        }

        return output;
    }

    /**
     * 构建公开详情页的归档 Bug 简报摘要（仅返回最新一条已归档简报）
     */
    private TicketPublicDetailOutput.ArchivedBugReportSummary buildArchivedBugReportSummary(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        List<BugReportTicketPO> reportLinks = bugReportTicketMapper.selectList(
                new LambdaQueryWrapper<BugReportTicketPO>()
                        .eq(BugReportTicketPO::getTicketId, ticketId)
                        .orderByDesc(BugReportTicketPO::getCreateTime)
        );
        if (reportLinks == null || reportLinks.isEmpty()) {
            return null;
        }

        Set<Long> reportIds = reportLinks.stream()
                .map(BugReportTicketPO::getReportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (reportIds.isEmpty()) {
            return null;
        }

        List<BugReportPO> reports = bugReportMapper.selectBatchIds(reportIds);
        if (reports == null || reports.isEmpty()) {
            return null;
        }
        Map<Long, BugReportPO> reportMap = reports.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(BugReportPO::getId, r -> r));

        BugReportPO latestArchivedReport = null;
        for (BugReportTicketPO link : reportLinks) {
            BugReportPO report = reportMap.get(link.getReportId());
            if (report != null && BugReportStatus.ARCHIVED.getCode().equals(report.getStatus())) {
                latestArchivedReport = report;
                break;
            }
        }
        if (latestArchivedReport == null) {
            return null;
        }

        TicketPublicDetailOutput.ArchivedBugReportSummary summary =
                new TicketPublicDetailOutput.ArchivedBugReportSummary();
        summary.setId(latestArchivedReport.getId());
        summary.setReportNo(latestArchivedReport.getReportNo());
        summary.setStatus(latestArchivedReport.getStatus());
        BugReportStatus status = BugReportStatus.fromCode(latestArchivedReport.getStatus());
        summary.setStatusLabel(status != null ? status.getLabel() : latestArchivedReport.getStatus());
        summary.setDefectCategory(latestArchivedReport.getDefectCategory());
        summary.setSeverityLevel(latestArchivedReport.getSeverityLevel());
        summary.setLogicCauseLevel1(latestArchivedReport.getLogicCauseLevel1());
        summary.setLogicCauseLevel2(latestArchivedReport.getLogicCauseLevel2());
        summary.setLogicCauseDetail(latestArchivedReport.getLogicCauseDetail());
        summary.setProblemDesc(latestArchivedReport.getProblemDesc());
        summary.setImpactScope(latestArchivedReport.getImpactScope());
        summary.setSolution(latestArchivedReport.getSolution());
        summary.setTempSolution(latestArchivedReport.getTempSolution());
        summary.setReviewedAt(latestArchivedReport.getReviewedAt());
        summary.setUpdateTime(latestArchivedReport.getUpdateTime());

        List<BugReportResponsiblePO> responsibles = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>()
                        .eq(BugReportResponsiblePO::getReportId, latestArchivedReport.getId())
                        .orderByAsc(BugReportResponsiblePO::getCreateTime)
        );
        if (responsibles != null && !responsibles.isEmpty()) {
            Set<Long> userIds = responsibles.stream()
                    .map(BugReportResponsiblePO::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!userIds.isEmpty()) {
                List<SysUserPO> users = userMapper.selectBatchIds(userIds);
                if (users != null && !users.isEmpty()) {
                    Map<Long, String> userNameMap = users.stream()
                            .filter(u -> u.getId() != null)
                            .collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName));
                    String names = responsibles.stream()
                            .map(BugReportResponsiblePO::getUserId)
                            .map(userNameMap::get)
                            .filter(Objects::nonNull)
                            .filter(name -> !name.trim().isEmpty())
                            .collect(Collectors.joining("、"));
                    summary.setResponsibleUserNames(names);
                }
            }
        }
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long ticketId, TicketAssignInput input, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        List<Long> assigneeIds = resolveAssigneeIdsFromAssignInput(input);
        if (assigneeIds.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "请至少指定一名处理人");
        }

        if (TicketStatus.fromCode(ticket.getStatus()) == TicketStatus.PENDING_ASSIGN
                || (ticket.getWorkflowId() != null && ticket.getWorkflowId() == ALERT_WORKFLOW_ID
                        && TicketStatus.ALERT_TRIGGERED.getCode().equalsIgnoreCase(ticket.getStatus()))) {
            ticketWorkflowAppService.assignFromPendingDispatch(
                    ticketId, assigneeIds, input.getRemark(), currentUserId);
            log.info("工单分派(待分派/待认领池→下一节点): ticketId={}, assigneeIds={}", ticketId, assigneeIds);
            return;
        }

        if (Boolean.TRUE.equals(input.getMergeAssignees())
                && ticket.getWorkflowId() != null && ticket.getWorkflowId() == DEFECT_WORKFLOW_ID
                && TicketStatus.fromCode(ticket.getStatus()) == TicketStatus.TESTING) {
            LinkedHashSet<Long> merged = new LinkedHashSet<>(ticketAssigneeSyncService.listActiveUserIds(ticketId));
            merged.addAll(assigneeIds);
            assigneeIds = new ArrayList<>(merged);
        }

        Long oldAssigneeId = ticket.getAssigneeId();
        ticketAssigneeSyncService.applyAssigneesToTicket(ticket, assigneeIds);
        ticketMapper.updateById(ticket);

        String oldValue = oldAssigneeId != null ? String.valueOf(oldAssigneeId) : "";
        String newValue = ticket.getAssigneeId() != null ? String.valueOf(ticket.getAssigneeId()) : "";
        recordLog(ticketId, currentUserId, TicketAction.ASSIGN.getCode(),
                oldValue, newValue, input.getRemark());
        ticketTimeTrackService.recordAssign(ticketId, currentUserId, oldAssigneeId, ticket.getAssigneeId(),
                ticket.getStatus(), ticket.getStatus(), input.getRemark());

        safePublishEvent(new TicketAssignedEvent(ticketId, ticket.getAssigneeId(),
                oldAssigneeId, currentUserId, TicketAssignType.MANUAL_ASSIGN.getCode()));

        log.info("工单分派: ticketId={}, assigneeIds={}", ticketId, assigneeIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long ticketId, TicketProcessInput input, Long currentUserId) {
        // 委托给工作流应用服务执行，确保经过工作流引擎校验（含角色检查）
        TransitInput transitInput = new TransitInput();
        transitInput.setTargetStatus(input.getTargetStatus() != null
                ? input.getTargetStatus().toLowerCase() : null);
        transitInput.setNewAssigneeId(input.getTargetUserId());
        transitInput.setRemark(input.getRemark());

        ticketWorkflowAppService.transit(ticketId, transitInput, currentUserId);

        if (input.getRemark() != null && !input.getRemark().isEmpty()) {
            recordOperationComment(ticketId, currentUserId, input.getRemark());
        }

        log.info("工单处理（委托工作流）: ticketId={}, targetStatus={}", ticketId, input.getTargetStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long ticketId, TicketCloseInput input, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        String fromStatus = ticket.getStatus();

        // 通过工作流引擎执行关闭流转（确保经过校验，不绕过）
        TransitInput transitInput = new TransitInput();
        transitInput.setTargetStatus(TicketStatus.CLOSED.getCode());
        transitInput.setRemark(input != null ? input.getRemark() : null);

        try {
            ticketWorkflowAppService.transit(ticketId, transitInput, currentUserId);
        } catch (BusinessException e) {
            // 如果工作流校验失败（例如管理员强制关闭），记录日志后抛出
            log.warn("工单[{}]关闭时工作流校验失败（fromStatus={}）: {}",
                    ticketId, fromStatus, e.getMessage());
            throw e;
        }

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
        output.setUrgeCount(ticket.getUrgeCount() != null ? ticket.getUrgeCount() : 0);
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

        List<Long> assigneeIdList = ticketAssigneeSyncService.listActiveUserIds(ticket.getId());
        if (assigneeIdList.isEmpty() && ticket.getAssigneeId() != null) {
            assigneeIdList = new ArrayList<>(Collections.singletonList(ticket.getAssigneeId()));
        }
        output.setAssigneeIds(assigneeIdList);
        output.setUrgeDefaultNotifyUserIds(new ArrayList<>(assigneeIdList));
        for (Long aid : assigneeIdList) {
            userIds.add(aid);
        }

        if (ticket.getCategoryId() != null) {
            TicketCategoryPO category = categoryMapper.selectById(ticket.getCategoryId());
            if (category != null) {
                output.setCategoryName(category.getName());
                output.setCategoryFullPath(buildCategoryFullPath(category));
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
        List<String> assigneeNames = new ArrayList<>();
        for (Long aid : assigneeIdList) {
            SysUserPO u = userMap.get(aid);
            if (u != null && u.getName() != null) {
                assigneeNames.add(u.getName());
            }
        }
        if (!assigneeNames.isEmpty()) {
            output.setAssigneeName(String.join("、", assigneeNames));
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
                ao.setSource(a.getSource());
                ao.setWecomMsgId(a.getWecomMsgId());
                AttachmentSource attachmentSource = AttachmentSource.fromCode(a.getSource());
                ao.setSourceLabel(attachmentSource.getLabel());
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

        output.setBugSummaryInfo(buildBugSummaryInfo(ticket));

        return output;
    }

    /**
     * 批量加载工单处理人明细（多人时列表展示用），按 sort_order、id 排序；无明细表数据时返回空映射。
     */
    private Map<Long, List<Long>> loadAssigneeUserIdsByTicketId(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<TicketAssigneePO> q = new LambdaQueryWrapper<>();
        q.in(TicketAssigneePO::getTicketId, ticketIds)
                .orderByAsc(TicketAssigneePO::getSortOrder)
                .orderByAsc(TicketAssigneePO::getId);
        List<TicketAssigneePO> rows = ticketAssigneeMapper.selectList(q);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> map = new LinkedHashMap<>();
        for (TicketAssigneePO row : rows) {
            if (row == null || row.getTicketId() == null || row.getUserId() == null) {
                continue;
            }
            map.computeIfAbsent(row.getTicketId(), k -> new ArrayList<>()).add(row.getUserId());
        }
        return map;
    }

    private static Map<Long, String> buildMultiAssigneeDisplayNames(
            Map<Long, List<Long>> assigneeUserIdsByTicketId,
            Map<Long, String> userNameMap) {
        if (assigneeUserIdsByTicketId == null || assigneeUserIdsByTicketId.isEmpty()
                || userNameMap == null || userNameMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> out = new HashMap<>();
        for (Map.Entry<Long, List<Long>> e : assigneeUserIdsByTicketId.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            List<String> names = new ArrayList<>();
            for (Long uid : e.getValue()) {
                if (uid == null) {
                    continue;
                }
                String name = userNameMap.get(uid);
                if (name != null && !name.trim().isEmpty()) {
                    names.add(name.trim());
                }
            }
            if (!names.isEmpty()) {
                out.put(e.getKey(), String.join("、", names));
            }
        }
        return out;
    }

    private TicketListOutput convertToListOutput(TicketPO po,
                                                  Map<Long, String> userNameMap,
                                                  Map<Long, String> categoryNameMap,
                                                  Map<Long, String> companyNameByTicketId,
                                                  Map<Long, String> multiAssigneeNameByTicketId,
                                                  Map<Long, String> slaStatusByTicketId) {
        TicketListOutput output = new TicketListOutput();
        output.setId(po.getId());
        output.setTicketNo(po.getTicketNo());
        output.setTitle(po.getTitle());
        if (po.getId() != null && companyNameByTicketId != null) {
            output.setCompanyName(companyNameByTicketId.get(po.getId()));
        }
        output.setCategoryId(po.getCategoryId());
        output.setCategoryName(categoryNameMap.get(po.getCategoryId()));
        output.setPriority(po.getPriority());
        output.setStatus(po.getStatus());
        output.setCreatorId(po.getCreatorId());
        output.setCreatorName(userNameMap.get(po.getCreatorId()));
        output.setAssigneeId(po.getAssigneeId());
        String multiAssignee = po.getId() != null && multiAssigneeNameByTicketId != null
                ? multiAssigneeNameByTicketId.get(po.getId()) : null;
        if (multiAssignee != null && !multiAssignee.isEmpty()) {
            output.setAssigneeName(multiAssignee);
        } else {
            output.setAssigneeName(userNameMap.get(po.getAssigneeId()));
        }
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

        if (po.getId() != null && slaStatusByTicketId != null) {
            String slaStatus = slaStatusByTicketId.get(po.getId());
            if (slaStatus != null) {
                output.setSlaStatus(slaStatus);
                output.setSlaStatusLabel(getSlaStatusLabel(slaStatus));
            }
        }

        return output;
    }

    private static final String SLA_STATUS_BREACHED = "BREACHED";
    private static final String SLA_STATUS_WARNING = "WARNING";
    private static final String SLA_STATUS_NORMAL = "NORMAL";

    private String getSlaStatusLabel(String slaStatus) {
        if (SLA_STATUS_BREACHED.equals(slaStatus)) {
            return "已超时";
        }
        if (SLA_STATUS_WARNING.equals(slaStatus)) {
            return "预警中";
        }
        if (SLA_STATUS_NORMAL.equals(slaStatus)) {
            return "正常";
        }
        return null;
    }

    /**
     * 批量查询工单的 SLA 状态（BREACHED > WARNING > NORMAL）
     * 取每张工单所有 sla_timer 中最严重的状态
     */
    private Map<Long, String> resolveSlaStatusByTicketId(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO> timers =
                slaTimerMapper.selectByTicketIds(ticketIds);
        if (timers == null || timers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO>> grouped =
                timers.stream()
                        .filter(t -> t.getTicketId() != null)
                        .collect(Collectors.groupingBy(
                                com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO::getTicketId));

        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, List<com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO>> entry : grouped.entrySet()) {
            String worst = SLA_STATUS_NORMAL;
            for (com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO timer : entry.getValue()) {
                if (timer.getIsBreached() != null && timer.getIsBreached() == 1) {
                    worst = SLA_STATUS_BREACHED;
                    break;
                }
                if (timer.getIsWarned() != null && timer.getIsWarned() == 1
                        && !SLA_STATUS_BREACHED.equals(worst)) {
                    worst = SLA_STATUS_WARNING;
                }
            }
            result.put(entry.getKey(), worst);
        }
        return result;
    }

    private List<Long> resolveAssigneeIdsFromAssignInput(TicketAssignInput input) {
        List<Long> raw = new ArrayList<>();
        if (input.getAssigneeIds() != null) {
            for (Long id : input.getAssigneeIds()) {
                if (id != null) {
                    raw.add(id);
                }
            }
        }
        LinkedHashSet<Long> deduped = new LinkedHashSet<>(raw);
        if (!deduped.isEmpty()) {
            return new ArrayList<>(deduped);
        }
        if (input.getAssigneeId() != null) {
            return Collections.singletonList(input.getAssigneeId());
        }
        return Collections.emptyList();
    }

    private void recordLog(Long ticketId, Long userId, String action,
                           String oldValue, String newValue, String remark) {
        TicketLogPO logPO = new TicketLogPO();
        logPO.setTicketId(ticketId);
        logPO.setUserId(userId != null ? userId : 0L);
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

    private void safePublishEvent(Object event) {
        if (event == null) {
            log.warn("事件对象为空，已跳过事件发布");
            return;
        }
        if (eventPublisher == null) {
            log.error("事件发布器未注入，已跳过事件发布: eventType={}", event.getClass().getSimpleName());
            return;
        }
        try {
            if (event instanceof DomainEvent) {
                DomainEvent domainEvent = (DomainEvent) event;
                log.info("发布领域事件: eventType={}, eventId={}, occurredAt={}",
                        domainEvent.getEventType(), domainEvent.getEventId(), domainEvent.getOccurredAt());
            } else {
                log.info("发布事件: eventType={}", event.getClass().getSimpleName());
            }
            eventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("事件发布失败，已降级跳过: eventType={}", event.getClass().getSimpleName(), ex);
        }
    }

    private void safeRecordCreateTrack(TicketPO ticket, Long currentUserId) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        try {
            ticketTimeTrackService.recordCreate(ticket.getId(), currentUserId, ticket.getStatus(),
                    "创建工单: " + ticket.getTicketNo());
        } catch (Exception ex) {
            // 时间追踪为衍生链路，失败时降级，避免影响主创建流程
            log.error("记录工单创建时间追踪失败，已降级跳过: ticketId={}", ticket.getId(), ex);
        }
    }

    /**
     * 组装缺陷维度摘要信息（从 bug_report 关联获取）
     * 取与该工单关联的最新一条简报作为数据源
     * 若工单未关联简报则返回 null
     */
    private BugSummaryInfoOutput buildBugSummaryInfo(TicketPO ticket) {
        BugReportTicketPO latestReportTicket = bugReportTicketMapper.selectOne(
                new LambdaQueryWrapper<BugReportTicketPO>()
                        .eq(BugReportTicketPO::getTicketId, ticket.getId())
                        .orderByDesc(BugReportTicketPO::getCreateTime)
                        .last("LIMIT 1")
        );
        if (latestReportTicket == null) {
            return null;
        }

        BugReportPO report = bugReportMapper.selectById(latestReportTicket.getReportId());
        if (report == null) {
            return null;
        }

        BugSummaryInfoOutput output = new BugSummaryInfoOutput();
        output.setBugReportId(report.getId());
        output.setBugReportNo(report.getReportNo());
        output.setDefectCategory(report.getDefectCategory());

        List<BugReportResponsiblePO> responsibles = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>()
                        .eq(BugReportResponsiblePO::getReportId, report.getId())
        );
        if (responsibles != null && !responsibles.isEmpty()) {
            Set<Long> userIds = responsibles.stream()
                    .map(BugReportResponsiblePO::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                List<SysUserPO> users = userMapper.selectBatchIds(userIds);
                if (users != null && !users.isEmpty()) {
                    String names = users.stream()
                            .map(SysUserPO::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining("、"));
                    output.setResponsibleUserName(names);
                }
            }
        }

        boolean isValid = BugReportStatus.ARCHIVED.getCode().equals(report.getStatus());
        output.setIsValidReport(isValid ? "YES" : "NO");
        output.setIsValidReportLabel(isValid ? "是" : "否");

        if (ticket.getExpectedTime() != null && !isTerminalStatus(ticket.getStatus())) {
            output.setIsOverdue(new Date().after(ticket.getExpectedTime()));
        } else {
            output.setIsOverdue(false);
        }

        return output;
    }

    private boolean isTerminalStatus(String status) {
        return "COMPLETED".equalsIgnoreCase(status)
                || "CLOSED".equalsIgnoreCase(status)
                || "completed".equals(status)
                || "closed".equals(status);
    }

    /**
     * 根据分类实体构建完整的中文路径（例如：客服问题 > 物流相关 > 入库异常）
     */
    private String buildCategoryFullPath(TicketCategoryPO category) {
        List<String> names = new ArrayList<>();
        TicketCategoryPO current = category;
        while (current != null) {
            names.add(0, current.getName());
            if (current.getParentId() != null) {
                current = categoryMapper.selectById(current.getParentId());
            } else {
                current = null;
            }
        }
        return String.join(" > ", names);
    }
}
package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketUrgeApplicationService;
import com.miduo.cloud.ticket.application.wecom.model.WecomBotParseResult;
import com.miduo.cloud.ticket.common.enums.TicketSource;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.WecomBotCommandType;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 企微机器人指令执行服务
 */
@Service
public class WecomBotCommandService {

    private static final String HELP_MESSAGE =
            "📌 工单助手使用指南\n" +
                    "1. 创建工单：@工单助手 #分类路径 工单标题\n" +
                    "   优先级：紧急/高/中/低\n" +
                    "   描述：详细描述\n" +
                    "2. 分类列表：@工单助手 分类\n" +
                    "3. 查询工单：@工单助手 查询 WO-20260228-003\n" +
                    "4. 我的工单：@工单助手 我的工单\n" +
                    "5. 催办工单：@工单助手 催办 WO-20260228-003";

    private final SysUserMapper sysUserMapper;
    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketApplicationService ticketApplicationService;
    private final TicketUrgeApplicationService ticketUrgeApplicationService;
    private final WecomProperties wecomProperties;

    public WecomBotCommandService(SysUserMapper sysUserMapper,
                                  TicketMapper ticketMapper,
                                  TicketCategoryMapper ticketCategoryMapper,
                                  TicketApplicationService ticketApplicationService,
                                  TicketUrgeApplicationService ticketUrgeApplicationService,
                                  WecomProperties wecomProperties) {
        this.sysUserMapper = sysUserMapper;
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.ticketApplicationService = ticketApplicationService;
        this.ticketUrgeApplicationService = ticketUrgeApplicationService;
        this.wecomProperties = wecomProperties;
    }

    /**
     * 执行机器人指令
     *
     * @param parseResult 解析结果
     * @param chatId      来源群ID
     * @param fromWecomId 发送人企微ID
     * @param defaultCategoryId 群默认分类ID（可空）
     */
    public CommandHandleResult handle(WecomBotParseResult parseResult,
                                      String chatId,
                                      String fromWecomId,
                                      Long defaultCategoryId) {
        CommandHandleResult result = new CommandHandleResult();
        if (parseResult == null || !parseResult.isSuccess()) {
            result.setReplyContent(parseResult != null ? parseResult.getErrorMessage() : "❌ 消息解析失败");
            return result;
        }

        SysUserPO sender = findUserByWecomId(fromWecomId);
        if (sender == null) {
            result.setReplyContent("❌ 您尚未关联工单系统账号，请先访问系统完成企微授权登录");
            return result;
        }

        WecomBotCommandType commandType = parseResult.getCommandType();
        if (commandType == WecomBotCommandType.HELP) {
            result.setReplyContent(HELP_MESSAGE);
            return result;
        }
        if (commandType == WecomBotCommandType.CATEGORY) {
            result.setReplyContent(buildCategoryListMessage());
            return result;
        }
        if (commandType == WecomBotCommandType.QUERY) {
            result.setReplyContent(buildTicketDetailMessage(parseResult.getTicketNo()));
            return result;
        }
        if (commandType == WecomBotCommandType.MY_TICKETS) {
            result.setReplyContent(buildMyTodoTicketsMessage(sender.getId()));
            return result;
        }
        if (commandType == WecomBotCommandType.URGE) {
            result.setReplyContent(handleUrgeCommand(parseResult.getTicketNo(), sender.getId()));
            return result;
        }
        if (commandType == WecomBotCommandType.CREATE) {
            return handleCreateCommand(parseResult, chatId, sender.getId(), defaultCategoryId);
        }

        result.setReplyContent("❌ 暂不支持该指令，请发送“@工单助手 帮助”查看可用命令");
        return result;
    }

    private CommandHandleResult handleCreateCommand(WecomBotParseResult parseResult,
                                                    String chatId,
                                                    Long senderId,
                                                    Long defaultCategoryId) {
        CommandHandleResult result = new CommandHandleResult();

        Long categoryId = resolveCategoryId(parseResult.getCategoryPath(), defaultCategoryId);
        if (categoryId == null) {
            result.setReplyContent("❌ 分类不存在，请发送“@工单助手 分类”查看完整分类列表");
            return result;
        }

        TicketCreateInput input = new TicketCreateInput();
        input.setTitle(parseResult.getTitle());
        input.setDescription(parseResult.getDescription());
        input.setCategoryId(categoryId);
        input.setPriority(parseResult.getPriority() == null ? "medium" : parseResult.getPriority());
        input.setSource(TicketSource.WECOM_BOT.getCode());
        input.setSourceChatId(chatId);
        input.setCustomFields(parseResult.getCustomFields());

        Long ticketId = ticketApplicationService.createTicket(input, senderId);
        TicketPO ticket = ticketMapper.selectById(ticketId);

        String categoryName = buildCategoryPathName(categoryId);
        String ticketNo = ticket != null ? ticket.getTicketNo() : "";
        String publicLink = buildPublicTicketLink(ticketNo);
        result.setTicketId(ticketId);
        result.setReplyContent("✅ 工单创建成功\n" +
                "工单编号：" + safeValue(ticketNo) + "\n" +
                "标题：" + safeValue(parseResult.getTitle()) + "\n" +
                "分类：" + safeValue(categoryName) + "\n" +
                "优先级：" + safeValue(parseResult.getPriority()) + "\n" +
                "查看详情：" + publicLink);
        return result;
    }

    private String handleUrgeCommand(String ticketNo, Long currentUserId) {
        try {
            TicketPO ticket = ticketUrgeApplicationService.urgeByTicketNo(ticketNo, currentUserId);
            return "✅ 催办已发送：工单 " + ticket.getTicketNo();
        } catch (Exception ex) {
            return "❌ 催办失败：" + ex.getMessage();
        }
    }

    private String buildMyTodoTicketsMessage(Long userId) {
        List<TicketPO> tickets = ticketMapper.selectList(
                new LambdaQueryWrapper<TicketPO>()
                        .eq(TicketPO::getAssigneeId, userId)
                        .notIn(TicketPO::getStatus, "COMPLETED", "CLOSED")
                        .orderByDesc(TicketPO::getUpdateTime)
                        .last("LIMIT 5")
        );
        if (tickets == null || tickets.isEmpty()) {
            return "📭 当前没有待处理工单";
        }

        StringBuilder builder = new StringBuilder("📝 我的待处理工单（最近5条）\n");
        for (TicketPO ticket : tickets) {
            builder.append("- ")
                    .append(safeValue(ticket.getTicketNo()))
                    .append(" | ")
                    .append(safeValue(ticket.getTitle()))
                    .append(" | ")
                    .append(resolveStatusLabel(ticket.getStatus()))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String buildTicketDetailMessage(String ticketNo) {
        if (ticketNo == null || ticketNo.trim().isEmpty()) {
            return "❌ 查询失败：工单编号不能为空";
        }
        TicketPO ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<TicketPO>()
                        .eq(TicketPO::getTicketNo, ticketNo.trim())
                        .last("LIMIT 1")
        );
        if (ticket == null) {
            return "❌ 工单不存在：" + ticketNo;
        }

        String assigneeName = "-";
        if (ticket.getAssigneeId() != null) {
            SysUserPO assignee = sysUserMapper.selectById(ticket.getAssigneeId());
            if (assignee != null && assignee.getName() != null) {
                assigneeName = assignee.getName();
            }
        }

        return "🔎 工单详情\n" +
                "编号：" + safeValue(ticket.getTicketNo()) + "\n" +
                "标题：" + safeValue(ticket.getTitle()) + "\n" +
                "状态：" + resolveStatusLabel(ticket.getStatus()) + "\n" +
                "处理人：" + assigneeName;
    }

    private String buildCategoryListMessage() {
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .eq(TicketCategoryPO::getIsActive, 1)
                        .orderByAsc(TicketCategoryPO::getLevel)
                        .orderByAsc(TicketCategoryPO::getSortOrder)
        );
        if (categories == null || categories.isEmpty()) {
            return "📭 当前暂无可用分类";
        }

        Map<Long, TicketCategoryPO> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategoryPO::getId, item -> item));

        StringBuilder builder = new StringBuilder("📚 可用分类列表\n");
        for (TicketCategoryPO category : categories) {
            String fullPath = buildCategoryPathName(category, categoryMap);
            builder.append("- ").append(fullPath).append('\n');
        }
        return builder.toString().trim();
    }

    private Long resolveCategoryId(String categoryPath, Long defaultCategoryId) {
        if ((categoryPath == null || categoryPath.trim().isEmpty()) && defaultCategoryId != null) {
            return defaultCategoryId;
        }
        if (categoryPath == null || categoryPath.trim().isEmpty()) {
            return null;
        }

        String normalizedInput = normalizeCategoryPath(categoryPath);
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .eq(TicketCategoryPO::getIsActive, 1)
        );
        if (categories == null || categories.isEmpty()) {
            return null;
        }

        Map<Long, TicketCategoryPO> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategoryPO::getId, item -> item));
        for (TicketCategoryPO category : categories) {
            String fullPath = normalizeCategoryPath(buildCategoryPathName(category, categoryMap));
            if (normalizedInput.equals(fullPath) || normalizedInput.equals(normalizeCategoryPath(category.getName()))) {
                return category.getId();
            }
        }
        return null;
    }

    private String buildCategoryPathName(Long categoryId) {
        if (categoryId == null) {
            return "-";
        }
        TicketCategoryPO target = ticketCategoryMapper.selectById(categoryId);
        if (target == null) {
            return "-";
        }
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .eq(TicketCategoryPO::getIsActive, 1)
        );
        if (categories == null || categories.isEmpty()) {
            return target.getName();
        }
        Map<Long, TicketCategoryPO> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategoryPO::getId, item -> item));
        return buildCategoryPathName(target, categoryMap);
    }

    private String buildCategoryPathName(TicketCategoryPO category, Map<Long, TicketCategoryPO> categoryMap) {
        List<String> names = new ArrayList<>();
        TicketCategoryPO current = category;
        while (current != null) {
            names.add(0, current.getName());
            Long parentId = current.getParentId();
            current = parentId == null ? null : categoryMap.get(parentId);
        }
        return String.join("/", names);
    }

    private String normalizeCategoryPath(String value) {
        return value == null ? "" : value.trim().replace(">", "/").replace(" ", "");
    }

    private String resolveStatusLabel(String statusCode) {
        if (statusCode == null) {
            return "-";
        }
        TicketStatus status = TicketStatus.fromCode(statusCode.toLowerCase());
        return status == null ? statusCode : status.getLabel();
    }

    private SysUserPO findUserByWecomId(String wecomUserId) {
        if (wecomUserId == null || wecomUserId.trim().isEmpty()) {
            return null;
        }
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getWecomUserid, wecomUserId.trim())
                        .last("LIMIT 1")
        );
    }

    private String safeValue(String value) {
        return value == null ? "-" : value;
    }

    private String buildPublicTicketLink(String ticketNo) {
        if (ticketNo == null || ticketNo.trim().isEmpty()) {
            return "-";
        }
        String domain = wecomProperties.getTrustedDomain();
        if (domain == null || domain.trim().isEmpty()) {
            return "-";
        }
        String normalizedDomain = domain.trim();
        if (!normalizedDomain.startsWith("http://") && !normalizedDomain.startsWith("https://")) {
            normalizedDomain = "https://" + normalizedDomain;
        }
        return normalizedDomain + "/open/ticket/" + ticketNo.trim();
    }

    /**
     * 指令执行结果
     */
    @lombok.Data
    public static class CommandHandleResult {
        private Long ticketId;
        private String replyContent;
    }
}

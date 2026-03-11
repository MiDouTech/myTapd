package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.wecom.model.WecomDraftSession;
import com.miduo.cloud.ticket.common.enums.TicketSource;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企微自然语言建单交互式确认流程处理器
 * Task023：企微文本消息自动创建工单
 */
@Service
public class WecomInteractiveConfirmService {

    private static final String CMD_CONFIRM = "1";
    private static final String CMD_MODIFY_CATEGORY = "2";
    private static final String CMD_MODIFY_PRIORITY = "3";
    private static final String CMD_SUPPLEMENT_DESC = "4";
    private static final String CMD_CANCEL = "0";

    private final WecomDraftSessionService draftSessionService;
    private final TicketApplicationService ticketApplicationService;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;

    public WecomInteractiveConfirmService(WecomDraftSessionService draftSessionService,
                                          TicketApplicationService ticketApplicationService,
                                          TicketCategoryMapper ticketCategoryMapper,
                                          TicketMapper ticketMapper,
                                          SysUserMapper sysUserMapper) {
        this.draftSessionService = draftSessionService;
        this.ticketApplicationService = ticketApplicationService;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.ticketMapper = ticketMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 处理用户回复指令（草稿确认流程中）
     *
     * @param content      用户回复内容
     * @param draft        当前草稿会话
     * @param chatId       来源群ID（群聊时使用）
     * @param wecomUserId  发送人企微UserId
     * @return 回复内容
     */
    public String handleReply(String content, WecomDraftSession draft,
                               String chatId, String wecomUserId) {
        if (content == null || draft == null) {
            return "❌ 会话状态异常，请重新发送消息创建工单";
        }

        String trimmedContent = content.trim();

        if (WecomDraftSession.Step.MODIFY_CATEGORY == draft.getStep()) {
            return handleModifyCategoryInput(trimmedContent, draft, chatId, wecomUserId);
        }
        if (WecomDraftSession.Step.MODIFY_PRIORITY == draft.getStep()) {
            return handleModifyPriorityInput(trimmedContent, draft, chatId, wecomUserId);
        }
        if (WecomDraftSession.Step.SUPPLEMENT_DESC == draft.getStep()) {
            return handleSupplementDescInput(trimmedContent, draft, chatId, wecomUserId);
        }

        if (CMD_CONFIRM.equals(trimmedContent)) {
            return handleConfirm(draft, chatId, wecomUserId);
        }
        if (CMD_MODIFY_CATEGORY.equals(trimmedContent)) {
            return handleStartModifyCategory(draft, chatId, wecomUserId);
        }
        if (CMD_MODIFY_PRIORITY.equals(trimmedContent)) {
            return handleStartModifyPriority(draft, chatId, wecomUserId);
        }
        if (CMD_SUPPLEMENT_DESC.equals(trimmedContent)) {
            return handleStartSupplementDesc(draft, chatId, wecomUserId);
        }
        if (CMD_CANCEL.equals(trimmedContent)) {
            return handleCancel(chatId, wecomUserId);
        }

        return "⚠️ 请回复有效指令：\n1=确认创建  2=修改分类  3=修改优先级  4=补充描述  0=取消";
    }

    private String handleConfirm(WecomDraftSession draft, String chatId, String wecomUserId) {
        SysUserPO sender = findUserByWecomId(wecomUserId);
        if (sender == null) {
            draftSessionService.removeDraft(chatId, wecomUserId);
            return "❌ 您尚未关联工单系统账号，请先访问系统完成企微授权登录";
        }

        Long categoryId = resolveCategoryId(draft.getCategoryPath());
        if (categoryId == null) {
            draftSessionService.removeDraft(chatId, wecomUserId);
            return "❌ 分类不存在，请发送 @工单助手 分类 查看完整分类列表后重新建单";
        }

        TicketCreateInput input = new TicketCreateInput();
        input.setTitle(draft.getTitle());
        input.setDescription(draft.getDescription() != null ? draft.getDescription() : draft.getTitle());
        input.setCategoryId(categoryId);
        input.setPriority(draft.getPriority() != null ? draft.getPriority() : "medium");
        input.setSource(TicketSource.WECOM_BOT.getCode());
        input.setSourceChatId(chatId);

        Long ticketId = ticketApplicationService.createTicket(input, sender.getId());
        TicketPO ticket = ticketMapper.selectById(ticketId);

        draftSessionService.removeDraft(chatId, wecomUserId);
        return "✅ 工单创建成功\n" +
                "工单编号：" + safeValue(ticket != null ? ticket.getTicketNo() : "") + "\n" +
                "标题：" + safeValue(draft.getTitle()) + "\n" +
                "分类：" + safeValue(draft.getCategoryPath()) + "\n" +
                "优先级：" + safeValue(draft.getPriority()) + "\n" +
                "请到系统内查看详情。";
    }

    private String handleStartModifyCategory(WecomDraftSession draft, String chatId, String wecomUserId) {
        draft.setStep(WecomDraftSession.Step.MODIFY_CATEGORY);
        draftSessionService.saveDraft(chatId, wecomUserId, draft, draft.isGroupChat());
        return buildCategoryListReplyMessage();
    }

    private String handleStartModifyPriority(WecomDraftSession draft, String chatId, String wecomUserId) {
        draft.setStep(WecomDraftSession.Step.MODIFY_PRIORITY);
        draftSessionService.saveDraft(chatId, wecomUserId, draft, draft.isGroupChat());
        return "📌 请选择优先级，直接回复以下数字：\n1=紧急  2=高  3=中  4=低";
    }

    private String handleStartSupplementDesc(WecomDraftSession draft, String chatId, String wecomUserId) {
        draft.setStep(WecomDraftSession.Step.SUPPLEMENT_DESC);
        draftSessionService.saveDraft(chatId, wecomUserId, draft, draft.isGroupChat());
        return "✏️ 请直接回复补充描述内容，将作为工单描述使用";
    }

    private String handleCancel(String chatId, String wecomUserId) {
        draftSessionService.removeDraft(chatId, wecomUserId);
        return "✅ 已取消本次工单创建，如需重新建单请再次发送消息";
    }

    private String handleModifyCategoryInput(String input, WecomDraftSession draft, String chatId, String wecomUserId) {
        Long categoryId = resolveCategoryId(input);
        if (categoryId == null) {
            return "❌ 分类不存在，请重新输入，或回复 0 取消建单：\n" + buildCategoryListText();
        }
        draft.setCategoryPath(input);
        draft.setStep(WecomDraftSession.Step.PENDING_CONFIRM);
        draftSessionService.saveDraft(chatId, wecomUserId, draft, draft.isGroupChat());
        return buildDraftPreviewMessage(draft);
    }

    private String handleModifyPriorityInput(String input, WecomDraftSession draft, String chatId, String wecomUserId) {
        String priority;
        switch (input) {
            case "1":
                priority = "urgent";
                break;
            case "2":
                priority = "high";
                break;
            case "3":
                priority = "medium";
                break;
            case "4":
                priority = "low";
                break;
            default:
                return "❌ 无效选择，请回复 1=紧急  2=高  3=中  4=低";
        }
        draft.setPriority(priority);
        draft.setStep(WecomDraftSession.Step.PENDING_CONFIRM);
        draftSessionService.saveDraft(chatId, wecomUserId, draft, draft.isGroupChat());
        return buildDraftPreviewMessage(draft);
    }

    private String handleSupplementDescInput(String input, WecomDraftSession draft, String chatId, String wecomUserId) {
        if (input.isEmpty()) {
            return "❌ 描述内容不能为空，请重新输入：";
        }
        draft.setDescription(input);
        draft.setStep(WecomDraftSession.Step.PENDING_CONFIRM);
        draftSessionService.saveDraft(chatId, wecomUserId, draft, draft.isGroupChat());
        return buildDraftPreviewMessage(draft);
    }

    /**
     * 构建草稿预览消息
     */
    public String buildDraftPreviewMessage(WecomDraftSession draft) {
        return "📋 工单预览（请确认）\n" +
                "标题：" + safeValue(draft.getTitle()) + "\n" +
                "分类：" + safeValue(draft.getCategoryPath()) + "\n" +
                "优先级：" + formatPriority(draft.getPriority()) + "\n" +
                "描述：" + safeValue(draft.getDescription()) + "\n" +
                "---\n" +
                "回复指令：\n" +
                "1=确认创建  2=修改分类  3=修改优先级  4=补充描述  0=取消";
    }

    private String buildCategoryListReplyMessage() {
        return "📚 请回复分类全路径（如：研发需求/缺陷修复）：\n" + buildCategoryListText() +
                "\n---\n回复 0 取消建单";
    }

    private String buildCategoryListText() {
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .eq(TicketCategoryPO::getIsActive, 1)
                        .orderByAsc(TicketCategoryPO::getLevel)
                        .orderByAsc(TicketCategoryPO::getSortOrder)
        );
        if (categories == null || categories.isEmpty()) {
            return "（暂无可用分类）";
        }
        Map<Long, TicketCategoryPO> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategoryPO::getId, c -> c));
        StringBuilder builder = new StringBuilder();
        for (TicketCategoryPO category : categories) {
            String path = buildCategoryPathName(category, categoryMap);
            builder.append("- ").append(path).append('\n');
        }
        return builder.toString().trim();
    }

    private Long resolveCategoryId(String categoryPath) {
        if (categoryPath == null || categoryPath.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizePath(categoryPath);
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>().eq(TicketCategoryPO::getIsActive, 1)
        );
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        Map<Long, TicketCategoryPO> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategoryPO::getId, c -> c));
        for (TicketCategoryPO category : categories) {
            String fullPath = normalizePath(buildCategoryPathName(category, categoryMap));
            if (normalized.equals(fullPath) || normalized.equals(normalizePath(category.getName()))) {
                return category.getId();
            }
        }
        return null;
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

    private String normalizePath(String value) {
        return value == null ? "" : value.trim().replace(">", "/").replace(" ", "");
    }

    private String formatPriority(String priority) {
        if (priority == null) {
            return "中";
        }
        switch (priority) {
            case "urgent": return "紧急";
            case "high": return "高";
            case "low": return "低";
            default: return "中";
        }
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
}

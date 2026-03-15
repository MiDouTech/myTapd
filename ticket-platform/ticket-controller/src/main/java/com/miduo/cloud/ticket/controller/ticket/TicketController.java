package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketBugApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketTimeTrackApplicationService;
import com.miduo.cloud.ticket.application.wecom.WecomMessageFieldParser;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.ticket.*;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomMessageParseInput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomMessageParseOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/ticket")
@Tag(name = "工单管理", description = "工单核心管理接口")
public class TicketController {

    @Resource
    private TicketApplicationService ticketService;

    @Resource
    private TicketBugApplicationService ticketBugApplicationService;

    @Resource
    private TicketTimeTrackApplicationService ticketTimeTrackApplicationService;

    @Resource
    private WecomMessageFieldParser wecomMessageFieldParser;

    /**
     * 创建工单
     * 接口编号：API000006
     * 产品文档功能：4.2.1 工单创建 - 选择分类→加载模板→填写→提交
     */
    @PostMapping("/create")
    @Operation(summary = "创建工单", description = "接口编号：API000006")
    public ApiResult<Long> createTicket(@Valid @RequestBody TicketCreateInput input) {
        Long currentUserId = getCurrentUserId();
        Long ticketId = ticketService.createTicket(input, currentUserId);
        return ApiResult.success(ticketId);
    }

    /**
     * 分页查询工单列表
     * 接口编号：API000007
     * 产品文档功能：4.2.2 工单列表与筛选 - 多视图(我创建的/我待办的/我参与的/我关注的/所有工单)
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询工单列表", description = "接口编号：API000007")
    public ApiResult<PageOutput<TicketListOutput>> getTicketPage(@Valid TicketPageInput input) {
        Long currentUserId = getCurrentUserId();
        PageOutput<TicketListOutput> page = ticketService.getTicketPage(input, currentUserId);
        return ApiResult.success(page);
    }

    /**
     * 获取工单详情
     * 接口编号：API000008
     * 产品文档功能：4.2.3 工单详情与操作 - 基本信息、描述、自定义字段、附件、评论
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取工单详情", description = "接口编号：API000008")
    public ApiResult<TicketDetailOutput> getTicketDetail(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        TicketDetailOutput detail = ticketService.getTicketDetail(id, currentUserId);
        return ApiResult.success(detail);
    }

    /**
     * 手动分派工单
     * 接口编号：API000009
     * 产品文档功能：4.5.1 分派策略 - 手动分派指定处理人
     */
    @PutMapping("/assign/{id}")
    @Operation(summary = "手动分派工单", description = "接口编号：API000009")
    public ApiResult<Void> assignTicket(@PathVariable Long id,
                                        @Valid @RequestBody TicketAssignInput input) {
        Long currentUserId = getCurrentUserId();
        ticketService.assignTicket(id, input, currentUserId);
        return ApiResult.success();
    }

    /**
     * 处理工单并流转
     * 接口编号：API000010
     * 产品文档功能：4.2.3 核心操作 - 处理、转派、挂起、恢复、验收通过/不通过
     */
    @PutMapping("/process/{id}")
    @Operation(summary = "处理工单并流转", description = "接口编号：API000010")
    public ApiResult<Void> processTicket(@PathVariable Long id,
                                         @Valid @RequestBody TicketProcessInput input) {
        Long currentUserId = getCurrentUserId();
        ticketService.processTicket(id, input, currentUserId);
        return ApiResult.success();
    }

    /**
     * 关闭工单
     * 接口编号：API000011
     * 产品文档功能：4.2.3 核心操作 - 关闭工单
     */
    @PutMapping("/close/{id}")
    @Operation(summary = "关闭工单", description = "接口编号：API000011")
    public ApiResult<Void> closeTicket(@PathVariable Long id,
                                       @RequestBody(required = false) TicketCloseInput input) {
        Long currentUserId = getCurrentUserId();
        ticketService.closeTicket(id, input, currentUserId);
        return ApiResult.success();
    }

    /**
     * 关注工单
     * 接口编号：API000012
     * 产品文档功能：4.2.3 核心操作 - 关注工单动态
     */
    @PostMapping("/follow/{id}")
    @Operation(summary = "关注工单", description = "接口编号：API000012")
    public ApiResult<Void> followTicket(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        ticketService.followTicket(id, currentUserId);
        return ApiResult.success();
    }

    /**
     * 取消关注工单
     * 接口编号：API000013
     * 产品文档功能：4.2.3 核心操作 - 取消关注工单
     */
    @DeleteMapping("/follow/{id}")
    @Operation(summary = "取消关注工单", description = "接口编号：API000013")
    public ApiResult<Void> unfollowTicket(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        ticketService.unfollowTicket(id, currentUserId);
        return ApiResult.success();
    }

    /**
     * 记录首次阅读轨迹
     * 接口编号：API000020
     * 产品文档功能：4.4.6 全链路时间追踪 - 首次阅读时间记录
     */
    @PostMapping("/{id}/track/read")
    @Operation(summary = "记录工单阅读轨迹", description = "接口编号：API000020")
    public ApiResult<Void> trackRead(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        ticketTimeTrackApplicationService.recordReadTrack(id, currentUserId);
        return ApiResult.success();
    }

    /**
     * 更新缺陷工单客服信息
     * 接口编号：API000021
     * 产品文档功能：4.2.3 缺陷工单详情页 - 客服信息区
     */
    @PutMapping("/bug/customer-info/{id}")
    @Operation(summary = "更新缺陷工单客服信息", description = "接口编号：API000021")
    public ApiResult<Void> updateBugCustomerInfo(@PathVariable Long id,
                                                 @RequestBody TicketBugCustomerInfoInput input) {
        Long currentUserId = getCurrentUserId();
        ticketBugApplicationService.updateCustomerInfo(id, input, currentUserId);
        return ApiResult.success();
    }

    /**
     * 更新缺陷工单测试信息
     * 接口编号：API000022
     * 产品文档功能：4.2.3 缺陷工单详情页 - 测试信息区
     */
    @PutMapping("/bug/test-info/{id}")
    @Operation(summary = "更新缺陷工单测试信息", description = "接口编号：API000022")
    public ApiResult<Void> updateBugTestInfo(@PathVariable Long id,
                                             @RequestBody TicketBugTestInfoInput input) {
        Long currentUserId = getCurrentUserId();
        ticketBugApplicationService.updateTestInfo(id, input, currentUserId);
        return ApiResult.success();
    }

    /**
     * 更新缺陷工单开发信息
     * 接口编号：API000023
     * 产品文档功能：4.2.3 缺陷工单详情页 - 开发信息区
     */
    @PutMapping("/bug/dev-info/{id}")
    @Operation(summary = "更新缺陷工单开发信息", description = "接口编号：API000023")
    public ApiResult<Void> updateBugDevInfo(@PathVariable Long id,
                                            @RequestBody TicketBugDevInfoInput input) {
        Long currentUserId = getCurrentUserId();
        ticketBugApplicationService.updateDevInfo(id, input, currentUserId);
        return ApiResult.success();
    }

    /**
     * 获取工单时间追踪链
     * 接口编号：API000024
     * 产品文档功能：4.4.6 全链路时间追踪 - 时间追踪链展示
     */
    @GetMapping("/{id}/time-track")
    @Operation(summary = "获取工单时间追踪链", description = "接口编号：API000024")
    public ApiResult<TicketTimeTrackOutput> getTimeTrack(@PathVariable Long id) {
        TicketTimeTrackOutput output = ticketTimeTrackApplicationService.getTimeTrack(id);
        return ApiResult.success(output);
    }

    /**
     * 获取工单节点耗时统计
     * 接口编号：API000025
     * 产品文档功能：4.4.6 全链路时间追踪 - 节点耗时统计
     */
    @GetMapping("/{id}/node-duration")
    @Operation(summary = "获取工单节点耗时统计", description = "接口编号：API000025")
    public ApiResult<TicketNodeDurationOutput> getNodeDuration(@PathVariable Long id) {
        TicketNodeDurationOutput output = ticketTimeTrackApplicationService.getNodeDuration(id);
        return ApiResult.success(output);
    }

    /**
     * 企微消息自然语言解析 - 客服信息字段提取
     * 接口编号：API000504
     * 产品文档功能：4.2.3 缺陷工单详情页 - 客服信息区企微消息一键解析赋值
     */
    @PostMapping("/wecom/parse-customer-info")
    @Operation(summary = "企微消息解析为客服信息字段", description = "接口编号：API000504")
    public ApiResult<WecomMessageParseOutput> parseWecomCustomerInfo(
            @Valid @RequestBody WecomMessageParseInput input) {
        WecomMessageParseOutput output = wecomMessageFieldParser.parse(input.getMessage());
        return ApiResult.success(output);
    }

    /**
     * 新增工单评论
     * 接口编号：API000508
     * 产品文档功能：工单详情 - 评论区发表评论
     */
    @PostMapping("/{id}/comment")
    @Operation(summary = "新增工单评论", description = "接口编号：API000508")
    public ApiResult<Long> addComment(@PathVariable Long id,
                                      @Valid @RequestBody TicketCommentInput input) {
        Long currentUserId = getCurrentUserId();
        Long commentId = ticketService.addComment(id, input.getContent(), currentUserId);
        return ApiResult.success(commentId);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

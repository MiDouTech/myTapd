package com.miduo.cloud.ticket.application.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.workflow.TicketWorkflowAppService;
import com.miduo.cloud.ticket.common.enums.TicketView;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.mcp.McpToolDefinition;
import com.miduo.cloud.ticket.entity.dto.mcp.McpToolResult;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketPageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具注册与执行（P0 只读）。
 *
 * <p>所有工具均以「当前调用者身份」执行，委托现有查询服务，权限随用户角色收敛；不暴露任何写操作。
 */
@Service
public class McpToolService {

    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    public static final String TOOL_LIST_MY_TICKETS = "list_my_tickets";
    public static final String TOOL_GET_TICKET = "get_ticket";
    public static final String TOOL_GET_FLOW_HISTORY = "get_ticket_flow_history";
    public static final String TOOL_QUERY_TICKETS = "query_tickets";
    public static final String TOOL_GET_AVAILABLE_ACTIONS = "get_available_actions";

    @Resource
    private TicketApplicationService ticketApplicationService;

    @Resource
    private TicketWorkflowAppService ticketWorkflowAppService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 返回 P0 只读工具清单（tools/list）。
     */
    public List<McpToolDefinition> listTools() {
        List<McpToolDefinition> tools = new ArrayList<>();

        Map<String, Object> listProps = new LinkedHashMap<>();
        listProps.put("view", stringProp(
                "视图：my_todo(我待办) / my_created(我创建) / my_participated(我参与) / my_followed(我关注) / defect(缺陷) / alert(告警) / all(全部,仅管理员)。默认 my_todo",
                "my_todo"));
        listProps.put("pageNum", intProp("页码，从1开始", 1));
        listProps.put("pageSize", intProp("每页条数，最大100", 20));
        tools.add(new McpToolDefinition(TOOL_LIST_MY_TICKETS,
                "按当前用户身份分页查询其工单列表（待办/创建/参与/关注等视图），范围与 Web 一致",
                objectSchema(listProps, null)));

        Map<String, Object> idProps = new LinkedHashMap<>();
        idProps.put("ticketId", longProp("工单ID"));
        tools.add(new McpToolDefinition(TOOL_GET_TICKET,
                "获取工单详情（标题、状态、处理人、优先级、描述、自定义字段、附件元数据、评论等）",
                objectSchema(idProps, new String[]{"ticketId"})));

        tools.add(new McpToolDefinition(TOOL_GET_FLOW_HISTORY,
                "获取工单流转历史（每次状态流转/转派/退回的记录）",
                objectSchema(idProps, new String[]{"ticketId"})));

        tools.add(new McpToolDefinition(TOOL_GET_AVAILABLE_ACTIONS,
                "获取工单当前可执行的工作流动作（P0 仅展示，不执行）",
                objectSchema(idProps, new String[]{"ticketId"})));

        Map<String, Object> queryProps = new LinkedHashMap<>();
        queryProps.put("keyword", stringProp("关键词：同时模糊匹配工单编号与标题", null));
        queryProps.put("status", stringProp("工单状态编码", null));
        queryProps.put("categoryId", longProp("分类ID"));
        queryProps.put("priority", stringProp("优先级：urgent/high/medium/low", null));
        queryProps.put("createTimeStart", stringProp("创建时间起（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）", null));
        queryProps.put("createTimeEnd", stringProp("创建时间止", null));
        queryProps.put("view", stringProp("视图，默认 all(仅管理员，否则降级为 my_created)", null));
        queryProps.put("pageNum", intProp("页码，从1开始", 1));
        queryProps.put("pageSize", intProp("每页条数，最大100", 20));
        tools.add(new McpToolDefinition(TOOL_QUERY_TICKETS,
                "按条件检索工单（关键词/状态/分类/优先级/时间范围），范围随当前用户权限收敛",
                objectSchema(queryProps, null)));

        return tools;
    }

    /**
     * 执行工具调用（tools/call）。参数错误或业务异常以 isError 结果返回，不抛出协议级错误。
     */
    public McpToolResult callTool(String name, JsonNode arguments, Long userId) {
        log.info("[MCP] tool call userId={} tool={}", userId, name);
        try {
            if (name == null) {
                return McpToolResult.ofError("缺少工具名称");
            }
            switch (name) {
                case TOOL_LIST_MY_TICKETS:
                    return listMyTickets(arguments, userId);
                case TOOL_GET_TICKET:
                    return getTicket(arguments, userId);
                case TOOL_GET_FLOW_HISTORY:
                    return getFlowHistory(arguments);
                case TOOL_GET_AVAILABLE_ACTIONS:
                    return getAvailableActions(arguments, userId);
                case TOOL_QUERY_TICKETS:
                    return queryTickets(arguments, userId);
                default:
                    return McpToolResult.ofError("未知工具: " + name);
            }
        } catch (IllegalArgumentException e) {
            return McpToolResult.ofError("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.warn("[MCP] tool call 失败 tool={} err={}", name, e.getMessage());
            return McpToolResult.ofError("工具执行失败: " + e.getMessage());
        }
    }

    // ---- 工具实现 ----

    private McpToolResult listMyTickets(JsonNode args, Long userId) throws Exception {
        TicketPageInput input = new TicketPageInput();
        input.setView(resolveView(optString(args, "view"), TicketView.MY_TODO.getCode()));
        applyPaging(input, args);
        return McpToolResult.ofText(toJson(ticketApplicationService.getTicketPage(input, userId)));
    }

    private McpToolResult queryTickets(JsonNode args, Long userId) throws Exception {
        TicketPageInput input = new TicketPageInput();
        input.setKeyword(optString(args, "keyword"));
        input.setStatus(optString(args, "status"));
        input.setPriority(optString(args, "priority"));
        input.setCategoryId(optLong(args, "categoryId"));
        input.setCreateTimeStart(optString(args, "createTimeStart"));
        input.setCreateTimeEnd(optString(args, "createTimeEnd"));
        input.setView(resolveView(optString(args, "view"), TicketView.MY_CREATED.getCode()));
        applyPaging(input, args);
        return McpToolResult.ofText(toJson(ticketApplicationService.getTicketPage(input, userId)));
    }

    private McpToolResult getTicket(JsonNode args, Long userId) throws Exception {
        Long id = requiredLong(args, "ticketId");
        return McpToolResult.ofText(toJson(ticketApplicationService.getTicketDetail(id, userId)));
    }

    private McpToolResult getFlowHistory(JsonNode args) throws Exception {
        Long id = requiredLong(args, "ticketId");
        return McpToolResult.ofText(toJson(ticketWorkflowAppService.getFlowHistory(id)));
    }

    private McpToolResult getAvailableActions(JsonNode args, Long userId) throws Exception {
        Long id = requiredLong(args, "ticketId");
        return McpToolResult.ofText(toJson(ticketWorkflowAppService.getAvailableActions(id, userId)));
    }

    // ---- 工具辅助 ----

    /**
     * 解析视图：未传则用 fallback；若解析为 all 但当前用户非管理员，则降级为 fallback，避免越权查看全部工单。
     */
    private String resolveView(String requested, String fallback) {
        String view = StringUtils.hasText(requested) ? requested.trim() : fallback;
        if (TicketView.ALL.getCode().equalsIgnoreCase(view) && !isAdmin()) {
            return fallback;
        }
        return view;
    }

    private boolean isAdmin() {
        List<String> roles = SecurityUtil.getCurrentUserRoles();
        if (roles == null) {
            return false;
        }
        for (String role : roles) {
            if (role == null) {
                continue;
            }
            String normalized = role.trim().toLowerCase();
            if ("admin".equals(normalized) || "ticket_admin".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void applyPaging(TicketPageInput input, JsonNode args) {
        Integer pageNum = optInt(args, "pageNum");
        Integer pageSize = optInt(args, "pageSize");
        if (pageNum != null && pageNum >= 1) {
            input.setPageNum(pageNum);
        }
        if (pageSize != null && pageSize >= 1) {
            input.setPageSize(Math.min(pageSize, 100));
        }
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // ---- JSON Schema 构造 ----

    private Map<String, Object> objectSchema(Map<String, Object> properties, String[] required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && required.length > 0) {
            schema.put("required", Arrays.asList(required));
        }
        return schema;
    }

    private Map<String, Object> stringProp(String description, String dft) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "string");
        p.put("description", description);
        if (dft != null) {
            p.put("default", dft);
        }
        return p;
    }

    private Map<String, Object> intProp(String description, Integer dft) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "integer");
        p.put("description", description);
        if (dft != null) {
            p.put("default", dft);
        }
        return p;
    }

    private Map<String, Object> longProp(String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "integer");
        p.put("description", description);
        return p;
    }

    // ---- 参数读取 ----

    private String optString(JsonNode args, String field) {
        JsonNode n = args == null ? null : args.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private Long optLong(JsonNode args, String field) {
        JsonNode n = args == null ? null : args.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asLong();
        }
        String s = n.asText().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("字段 " + field + " 不是合法整数");
        }
    }

    private Long requiredLong(JsonNode args, String field) {
        Long value = optLong(args, field);
        if (value == null) {
            throw new IllegalArgumentException("缺少必填参数: " + field);
        }
        return value;
    }

    private Integer optInt(JsonNode args, String field) {
        JsonNode n = args == null ? null : args.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asInt();
        }
        String s = n.asText().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("字段 " + field + " 不是合法整数");
        }
    }
}

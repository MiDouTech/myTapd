package com.miduo.cloud.ticket.controller.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.miduo.cloud.ticket.application.mcp.McpDispatchService;
import com.miduo.cloud.ticket.application.mcp.McpToolService;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.entity.dto.mcp.McpToolResult;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCommentInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class McpCommentToolTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TicketApplicationService tickets = mock(TicketApplicationService.class);
    private final McpToolService tools = new McpToolService();
    private final McpDispatchService dispatch = new McpDispatchService();
    private ValidatorFactory validators;

    @BeforeEach
    void setUp() {
        validators = Validation.buildDefaultValidatorFactory();
        ReflectionTestUtils.setField(tools, "ticketApplicationService", tickets);
        ReflectionTestUtils.setField(tools, "objectMapper", mapper);
        ReflectionTestUtils.setField(tools, "validator", validators.getValidator());
        ReflectionTestUtils.setField(dispatch, "mcpToolService", tools);
    }

    @AfterEach
    void close() { validators.close(); }

    @Test
    void advertisesCommentAndPreservesQueryTools() throws Exception {
        JsonNode result = mapper.valueToTree(dispatch.dispatch(mapper.readTree(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"), 7L));
        assertEquals(6, result.at("/result/tools").size());
        JsonNode tool = result.at("/result/tools/5");
        assertEquals("add_ticket_comment", tool.get("name").asText());
        assertEquals(mapper.readTree("[\"ticketId\",\"content\"]"), tool.at("/inputSchema/required"));
        assertEquals(20000, tool.at("/inputSchema/properties/content/maxLength").asInt());
    }

    @Test
    void dispatchesWithAuthenticatedAuthorAndReturnsCreatedId() throws Exception {
        when(tickets.addComment(eq(42L), any(TicketCommentInput.class), eq(7L))).thenReturn(99L);
        JsonNode request = mapper.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"add_ticket_comment\",\"arguments\":{\"ticketId\":42,\"content\":\"复测仍失败\",\"mentionedUserIds\":[8,9],\"userId\":123}}}");
        JsonNode result = mapper.valueToTree(dispatch.dispatch(request, 7L));
        assertFalse(result.at("/result/isError").asBoolean());
        assertEquals(99L, mapper.readTree(result.at("/result/content/0/text").asText()).get("commentId").asLong());
        verify(tickets).addComment(eq(42L), argThat(input ->
                "复测仍失败".equals(input.getContent()) && Arrays.asList(8L, 9L).equals(input.getMentionedUserIds())), eq(7L));
    }

    @Test
    void supportsCommentsWithoutMentions() throws Exception {
        when(tickets.addComment(eq(42L), any(), eq(7L))).thenReturn(99L);
        assertFalse(call("{\"ticketId\":42,\"content\":\"进展更新\"}").getIsError());
        verify(tickets).addComment(eq(42L), argThat(input -> input.getMentionedUserIds() == null), eq(7L));
    }

    @Test
    void rejectsInvalidArgumentsBeforeAnyWrite() throws Exception {
        for (String args : Arrays.asList("null", "{}", "{\"ticketId\":42}",
                "{\"ticketId\":42,\"content\":123}", "{\"ticketId\":42,\"content\":\"  \"}",
                "{\"ticketId\":0,\"content\":\"x\"}", "{\"ticketId\":1.2,\"content\":\"x\"}",
                "{\"ticketId\":9223372036854775808,\"content\":\"x\"}",
                "{\"ticketId\":42,\"content\":\"x\",\"mentionedUserIds\":\"8\"}",
                "{\"ticketId\":42,\"content\":\"x\",\"mentionedUserIds\":[null]}",
                "{\"ticketId\":42,\"content\":\"x\",\"mentionedUserIds\":[-1]}")) {
            assertTrue(call(args).getIsError(), args);
        }
        ObjectNode oversized = mapper.createObjectNode().put("ticketId", 42).put("content", new String(new char[20001]).replace('\0', 'x'));
        assertTrue(tools.callTool("add_ticket_comment", oversized, 7L).getIsError());
        oversized.put("content", "x");
        for (int i = 0; i < 21; i++) oversized.withArray("mentionedUserIds").add(i + 1);
        assertTrue(tools.callTool("add_ticket_comment", oversized, 7L).getIsError());
        verifyNoInteractions(tickets);
    }

    @Test
    void rejectsUnauthenticatedDispatch() throws Exception {
        JsonNode response = mapper.valueToTree(dispatch.dispatch(mapper.readTree(
                "{\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"add_ticket_comment\",\"arguments\":{\"ticketId\":42,\"content\":\"x\"}}}"), null));
        assertTrue(response.hasNonNull("error"));
        assertTrue(tools.callTool("add_ticket_comment", mapper.readTree("{\"ticketId\":42,\"content\":\"x\"}"), null).getIsError());
        verifyNoInteractions(tickets);
    }

    @Test
    void reportsServiceFailureWithoutRetrying() throws Exception {
        when(tickets.addComment(eq(42L), any(), eq(7L))).thenThrow(new IllegalStateException("工单不存在或无权操作"));
        assertTrue(call("{\"ticketId\":42,\"content\":\"x\"}").getIsError());
        verify(tickets, times(1)).addComment(eq(42L), any(), eq(7L));
    }

    private McpToolResult call(String args) throws Exception {
        return tools.callTool("add_ticket_comment", mapper.readTree(args), 7L);
    }
}

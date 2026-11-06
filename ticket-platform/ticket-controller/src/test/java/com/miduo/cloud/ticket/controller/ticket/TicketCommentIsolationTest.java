package com.miduo.cloud.ticket.controller.ticket;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketCommentPolicy;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketCommentMentionEvent;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCommentInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 执行真实迁移和 MyBatis 查询，覆盖历史数据与混合可见域的服务输出。 */
class TicketCommentIsolationTest {
    private TicketApplicationService service;
    private TicketCommentMapper comments;
    private SysUserMapper users;
    private ApplicationEventPublisher events;
    private JdbcTemplate jdbc;
    private SqlSession session;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE ticket_comment (id bigint AUTO_INCREMENT PRIMARY KEY, ticket_id bigint, user_id bigint, content text, type varchar(20), create_time timestamp DEFAULT CURRENT_TIMESTAMP, update_time timestamp, create_by varchar(100), update_by varchar(100), deleted int DEFAULT 0)");
        jdbc.update("INSERT INTO ticket_comment(ticket_id,user_id,content,type) VALUES (42,7,'legacy comment','COMMENT'), (42,7,'legacy operation','OPERATION'), (42,7,'legacy system','system')");
        new ResourceDatabasePopulator(new FileSystemResource("../ticket-bootstrap/src/main/resources/db/migration/V70__ticket_comment_visibility.sql")).execute(ds);
        new ResourceDatabasePopulator(new FileSystemResource("../ticket-bootstrap/src/main/resources/db/migration/V71__ticket_comment_rich_text.sql")).execute(ds);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(TicketCommentMapper.class);
        factory.setConfiguration(configuration);
        session = factory.getObject().openSession(true);
        comments = session.getMapper(TicketCommentMapper.class);

        service = new TicketApplicationService();
        for (Field field : TicketApplicationService.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Resource.class)) {
                ReflectionTestUtils.setField(service, field.getName(), mock(field.getType()));
            }
        }
        users = mock(SysUserMapper.class);
        when(users.selectById(7L)).thenReturn(employee(7L, 1, "wecom-7"));
        when(users.selectById(8L)).thenReturn(employee(8L, 1, "wecom-8"));
        when(users.selectById(9L)).thenReturn(employee(9L, 1, null));
        when(users.selectById(10L)).thenReturn(employee(10L, 0, "wecom-10"));
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "commentPolicy", new TicketCommentPolicy(users));
        ReflectionTestUtils.setField(service, "commentMapper", comments);
        events = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "eventPublisher", events);
        TicketMapper tickets = (TicketMapper) ReflectionTestUtils.getField(service, "ticketMapper");
        TicketPO ticket = new TicketPO();
        ticket.setId(42L);
        ticket.setTicketNo("MTS-42");
        ticket.setCreatorId(9L);
        when(tickets.selectById(42L)).thenReturn(ticket);
        when(tickets.selectOne(any())).thenReturn(ticket);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (session != null) session.close();
    }

    @Test
    void migrationPreservesEveryHistoricalTypeAndDefaultsNewRowsToInternal() {
        assertEquals(Arrays.asList("PUBLIC", "PUBLIC", "PUBLIC"), jdbc.queryForList("SELECT visibility FROM ticket_comment ORDER BY id", String.class));
        assertEquals(Arrays.asList("legacy comment", "legacy operation", "legacy system"), jdbc.queryForList("SELECT content FROM ticket_comment ORDER BY id", String.class));
        jdbc.update("INSERT INTO ticket_comment(ticket_id,content) VALUES (42,'new system')");
        assertEquals("INTERNAL", jdbc.queryForObject("SELECT visibility FROM ticket_comment WHERE content='new system'", String.class));
    }

    @Test
    void publicAndExternalDetailNeverReturnInternalRowsButEmployeeDetailDoes() {
        Long internalId = service.addComment(42L, input("<img src=x onerror=alert(1)> @name(8)", null), 7L);
        service.addComment(42L, input("public reply", "PUBLIC"), 7L);
        assertEquals(4, service.getPublicTicketDetail("MTS-42").getComments().size());
        assertTrue(service.getPublicTicketDetail("MTS-42").getComments().stream().noneMatch(c -> internalId.equals(c.getId())));
        assertEquals(4, service.getTicketDetail(42L, 9L).getComments().size());
        assertEquals(4, service.getTicketDetail(42L, 10L).getComments().size());
        assertEquals(4, service.getTicketDetail(42L, null).getComments().size());
        assertEquals(5, service.getTicketDetail(42L, 7L).getComments().size());
        assertTrue(service.getTicketDetail(42L, 7L).getCanWriteComments());
        assertFalse(service.getTicketDetail(42L, 9L).getCanWriteComments());
        assertEquals("<img src=x onerror=alert(1)> @name(8)", comments.selectById(internalId).getContent());
        verifyNoInteractions(events);
    }

    @Test
    void unauthorizedActorsAndInvalidInputsCannotWrite() {
        for (Long id : Arrays.asList(null, 9L, 10L, 999L)) {
            assertThrows(BusinessException.class, () -> service.addComment(42L, input("secret", null), id));
        }
        assertThrows(BusinessException.class, () -> service.addComment(42L, input("secret", "public"), 7L));
        assertThrows(BusinessException.class, () -> service.addComment(42L, input(" ", null), 7L));
        TicketCommentInput attachment = input("secret", null);
        attachment.setAttachments(Collections.singletonList("https://example.test/image.png"));
        assertThrows(BusinessException.class, () -> service.addComment(42L, attachment, 7L));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM ticket_comment", Integer.class));
    }

    @Test
    void internalMentionsRequireCurrentEmployeeAccessAndCarryNoBody() {
        TicketCommentInput input = input("confidential", null);
        input.setMentionedUserIds(Arrays.asList(8L, 8L, 7L));
        Long id = service.addComment(42L, input, 7L);
        ArgumentCaptor<TicketCommentMentionEvent> event = ArgumentCaptor.forClass(TicketCommentMentionEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(id, event.getValue().getCommentId());
        assertEquals(Collections.singletonList(8L), event.getValue().getMentionedUserIds());
        assertNull(event.getValue().getCommentPlainSummary());
        input.setMentionedUserIds(Collections.singletonList(9L));
        assertThrows(BusinessException.class, () -> service.addComment(42L, input, 7L));
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM ticket_comment", Integer.class));
    }

    @Test
    void apiKeyCannotPublishAndOwnedPluginWritesStayPublic() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("agent", null,
                Collections.singletonList(new SimpleGrantedAuthority("AGENT_API_KEY"))));
        Long internal = service.addComment(42L, input("analysis", "PUBLIC"), 7L);
        assertEquals("INTERNAL", comments.selectById(internal).getVisibility());
        SecurityContextHolder.clearContext();
        Long external = service.addPluginComment(42L, input("customer follow-up", "INTERNAL"), 9L);
        assertEquals("PUBLIC", comments.selectById(external).getVisibility());
        assertThrows(BusinessException.class, () -> service.addPluginComment(42L, input("forged", null), 8L));
    }

    private TicketCommentInput input(String content, String visibility) {
        TicketCommentInput input = new TicketCommentInput();
        input.setContent(content);
        input.setVisibility(visibility);
        return input;
    }

    @Test
    void richInternalContentIsSanitizedAndDoesNotReachPublicReaders() {
        TicketCommentInput input = input("<p style='color:red;background:url(https://bad.test/x)'><strong>内部结论</strong></p><table><tr><td>复现</td></tr></table><script>alert(1)</script>", "INTERNAL");
        input.setContentFormat("HTML");
        Long id = service.addComment(42L, input, 7L);
        TicketCommentPO row = comments.selectById(id);
        assertEquals("HTML", row.getContentFormat());
        assertTrue(row.getContent().contains("<strong>内部结论</strong>"));
        assertTrue(row.getContent().contains("<table>"));
        assertFalse(row.getContent().contains("script"));
        assertFalse(row.getContent().contains("bad.test"));
        assertEquals(3, service.getPublicTicketDetail("MTS-42").getComments().size());
        assertEquals(3, service.getTicketDetail(42L, 9L).getComments().size());
        assertTrue(service.getTicketDetail(42L, 7L).getComments().stream()
                .anyMatch(c -> id.equals(c.getId()) && "HTML".equals(c.getContentFormat())));
        verifyNoInteractions(events);
    }

    @Test
    void plainPublicInputIsEscapedForPublicConsumers() {
        TicketCommentInput input = input("<script>alert(1)</script>", "PUBLIC");
        input.setContentFormat("PLAIN_TEXT");
        Long id = service.addComment(42L, input, 7L);
        assertFalse(comments.selectById(id).getContent().contains("<script>"));
    }

    private SysUserPO employee(Long id, int status, String wecom) {
        SysUserPO user = new SysUserPO();
        user.setId(id);
        user.setAccountStatus(status);
        user.setWecomUserid(wecom);
        return user;
    }
}

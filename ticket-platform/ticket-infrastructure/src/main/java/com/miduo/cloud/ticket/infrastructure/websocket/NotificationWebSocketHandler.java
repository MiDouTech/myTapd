package com.miduo.cloud.ticket.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.ticket.domain.common.event.NotificationSendEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知WebSocket处理器
 * 管理用户WebSocket连接，实时推送通知消息
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("WebSocket连接建立: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
            log.info("WebSocket连接关闭: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("收到WebSocket消息: sessionId={}, payload={}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = extractUserId(session);
        log.error("WebSocket传输错误: userId={}, sessionId={}", userId, session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    /**
     * 监听通知发送事件，通过WebSocket实时推送
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationSend(NotificationSendEvent event) {
        Long userId = event.getUserId();
        WebSocketSession session = userSessions.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", event.getNotificationId());
            payload.put("type", event.getType());
            payload.put("title", event.getTitle());
            payload.put("content", event.getContent());
            payload.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
            log.debug("WebSocket通知已推送: userId={}, type={}", userId, event.getType());
        } catch (IOException e) {
            log.error("WebSocket消息发送失败: userId={}", userId, e);
        }
    }

    /**
     * 向指定用户推送消息
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("WebSocket消息发送失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 从WebSocket会话中提取用户ID
     * 通过URL查询参数 ?userId=xxx 传递
     */
    private Long extractUserId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("userId=")) {
            try {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        return Long.parseLong(param.substring("userId=".length()));
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("解析WebSocket用户ID失败: query={}", query);
            }
        }
        return null;
    }
}

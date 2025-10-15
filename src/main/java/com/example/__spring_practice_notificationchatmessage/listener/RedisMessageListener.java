package com.example.__spring_practice_notificationchatmessage.listener;

import com.example.__spring_practice_notificationchatmessage.model.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageListener implements MessageListener {
    // Will be changed into Custom WebSocketHandler
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            log.debug("Redis 메시지 수신 - 채널: {}, 내용: {}", channel, messageBody);

            if (channel.startsWith("notification:user:")) {
                // User Notification
            } else if (channel.startsWith("notification:system")) {
                // System Notification
            } else if (channel.startsWith("notification:group")) {
                // Group Notification
            } else if (channel.startsWith("chat:")) {
                // Chat Notification
            } else if (channel.equals("chatroom:created")) {
                // Generated Chat Room Notification
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
        }
    }

    private void handleUserNotification(String channel, String MessageBody) {
        try {
            String userId = channel.substring("notification:user:".length());

            NotificationMessage notification = objectMapper.readValue(messageBody, NotificationMessage.class);

            webSocketHandler.broadcast(notification);

            log.info("시스템 알림 처리 완료 - 타입: {}", notification.getType());
        } catch (Exception e) {
            log.error("사용자 알림 처리 실패", e);
        }
    }
}

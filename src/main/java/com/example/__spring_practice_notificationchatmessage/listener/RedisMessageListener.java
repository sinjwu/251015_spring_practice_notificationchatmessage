package com.example.__spring_practice_notificationchatmessage.listener;

import com.example.__spring_practice_notificationchatmessage.model.ChatMessage;
import com.example.__spring_practice_notificationchatmessage.model.NotificationMessage;
import com.example.__spring_practice_notificationchatmessage.websocket.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;

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
                handleUserNotification(channel, messageBody);
            } else if (channel.startsWith("notification:system")) {
                // System Notification
                handleSystemNotification(messageBody);
            } else if (channel.startsWith("notification:group")) {
                // Group Notification
                handleGroupNotification(channel, messageBody);
            } else if (channel.startsWith("chat:")) {
                // Chat Notification
                handleChatMessage(channel, messageBody);
            } else if (channel.equals("chatroom:created")) {
                // Generated Chat Room Notification
                handleRoomCreated(messageBody);
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
        }
    }

    private void handleUserNotification(String channel, String messageBody) {
        try {
            String userId = channel.substring("notification:user:".length());

            NotificationMessage notification = objectMapper.readValue(messageBody, NotificationMessage.class);

            webSocketHandler.sendToUser(userId, notification);

            log.info("시스템 알림 처리 완료 - 사용자: {}, 타입: {}", userId, notification.getType());
        } catch (Exception e) {
            log.error("사용자 알림 처리 실패", e);
        }
    }

    private void handleSystemNotification(String messageBody) {
        try {
            NotificationMessage notification = objectMapper.readValue(messageBody, NotificationMessage.class);

            webSocketHandler.broadcast(notification);

            log.info("시스템 알림 처리 완료 - 타입: {}", notification.getType());
        } catch (Exception e) {
            log.error("시스템 알림 처리 실패", e);
        }
    }

    private void handleGroupNotification(String channel, String messageBody) {
        try {
            String groupId = channel.substring("notification:group".length());

            NotificationMessage notification = objectMapper.readValue(messageBody, NotificationMessage.class);

            webSocketHandler.sendToGroup(groupId, notification);

            log.info("그룹 알림 처리 완료 - 그룹: {}, 타입: {}", groupId, notification.getType());
        } catch (Exception e) {
            log.error("그룹 알림 처리 실패", e);
        }
    }

    private void handleChatMessage(String channel, String messageBody) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(messageBody, ChatMessage.class);

            webSocketHandler.sendToRoom(chatMessage.getRoomId(), chatMessage);

            log.info("채팅 메시지 처리 완료 - 방: {}", chatMessage.getRoomId());
        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패, e");
        }
    }

    private void handleRoomCreated(String messageBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> roomCreatedEvent = objectMapper.readValue(messageBody, Map.class);

            webSocketHandler.broadcastRoomCreated(roomCreatedEvent);

            log.info("채팅방 생성 이벤트 처리 완료 - 방ID: {}, 방 이름: {}", roomCreatedEvent.get("roomId"), roomCreatedEvent.get("roomName"));
        } catch (Exception e) {
            log.error("채팅방 생성 이벤트 처리 실패", e);
        }
    }
}

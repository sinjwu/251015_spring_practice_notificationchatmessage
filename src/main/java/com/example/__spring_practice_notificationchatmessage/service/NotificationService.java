package com.example.__spring_practice_notificationchatmessage.service;

import com.example.__spring_practice_notificationchatmessage.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void sendNotificationToUser(String userId, NotificationMessage notification) {
        try {
            notification.setUserId(userId);
            String channel = "notification:user:" + userId;

            Long subscriberCount = redisTemplate.convertAndSend(channel, notification);
            log.info("알림 전송 완료 - 사용자: {}, 구독자 수: {}", userId, subscriberCount);

        } catch (Exception e) {
            log.error("알림 전송 실패 - 사용자: {}", userId, e);
        }
    }

    public void saveNotificationHistory(String userId, NotificationMessage notification) {
        try {
            String historyKey = "notification:history:" + userId;
            redisTemplate.opsForList().leftPush(historyKey, notification);
            redisTemplate.opsForList().trim(historyKey, 0, 99);
            redisTemplate.expire(historyKey, Duration.ofDays(30));
        } catch (Exception e) {
            log.error("알림 이력 저장 실패", e);
        }
    }
}

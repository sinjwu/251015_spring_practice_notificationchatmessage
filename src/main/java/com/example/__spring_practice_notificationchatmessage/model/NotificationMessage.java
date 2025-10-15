package com.example.__spring_practice_notificationchatmessage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String id;
    private String type;
    private String userId;
    private String title;
    private String content;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    private NotificationLevel level;

    public enum NotificationLevel {
        INFO, WARNING, ERROR, SUCCESS
    }

    public static NotificationMessage orderCreated(String userId, String orderId) {
        return NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("ORDER")
                .userId(userId)
                .title("주문 접수 완료")
                .content("주문번호 " + orderId + "가 정상적으로 접수되었습니다.")
                .level(NotificationLevel.SUCCESS)
                .timestamp(LocalDateTime.now())
                .data(Map.of("orderId", orderId))
                .build();
    }

    public static NotificationMessage paymentFailed(String userId, String orderId, String reason) {
        return NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("PAYMENT")
                .userId(userId)
                .title("결제 실패")
                .content("주문번호 " + orderId + "결제가 실패했습니다." + reason)
                .level(NotificationLevel.SUCCESS)
                .timestamp(LocalDateTime.now())
                .data(Map.of("orderId", orderId, "reason", reason))
                .build();
    }
}

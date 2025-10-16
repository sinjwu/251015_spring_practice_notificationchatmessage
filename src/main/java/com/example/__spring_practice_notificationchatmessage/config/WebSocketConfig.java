package com.example.__spring_practice_notificationchatmessage.config;

import com.example.__spring_practice_notificationchatmessage.websocket.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/notifications").setAllowedOriginPatterns("*");

        registry.addHandler(webSocketHandler, "/ws/chat").setAllowedOriginPatterns("*");

        log.info("WebSocket 엔드포인트 등록 완료");
        log.info("- /ws/notifications (알림)");
        log.info("- /ws/chat (채팅)");
    }
}

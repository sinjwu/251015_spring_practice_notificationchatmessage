package com.example.__spring_practice_notificationchatmessage.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomMembers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();

    // When client accessed
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Websocket 연결 성공 - 사용자: {}, 세션ID: {}", userId, session.getId());
        }
    }

    // When received message from client
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String userId = getUserIdFromSession(session);
        String payload = (String) message.getPayload();

        log.debug("Websocket 메시지 수신 - 사용자: {}, 내용: {}", userId, payload);

        processClientMessage(userId, payload);
    }

    // When network error occurred
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류", exception);
    }

    // When client disconnects
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    removeUserFromAllRooms(userId);
                    log.info("WebSocket 연결 종료 - 사용자: {} (모든 세션 종료)", userId);
                } else {
                    log.info("WebSocket 연결 종료 - 사용자: {}, 세션ID: {} (남은 세션: {}개)", userId, session.getId(), sessions.size());
                }
            }
        }
    }

    // Whether big sized message split into small parts or not
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void sendToUser(String userId, Object message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(json);

                int successCount = 0;
                for (WebSocketSession session: sessions) {
                    if (session.isOpen()) {
                        try {
                            synchronized (session) {
                                session.sendMessage(textMessage);
                            }
                            successCount++;
                        } catch (Exception e) {
                            log.error("사용자 메시지 전송 실패 - 사용자: {}, 세션ID: {}", userId, session.getId(), e);
                        }
                    }
                }
                log.debug("사용자 메시지 전송 완료 - 사용자: {}, 전송 세션 수: {}/{}", userId, successCount, sessions.size());
            } catch (Exception e) {
                log.error("사용자 메시지 직렬화 실패 - 사용자: {}", e);
            }
        }
    }

    public void broadcast(Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("브로드캐스트 메시지 직렬화 실패", e);
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        long totalSessions = userSessions.values().stream()
                .flatMap(Set::stream)
                .filter(WebSocketSession::isOpen)
                .peek(session -> {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (Exception e) {
                        log.error("브로드캐스트 전송 실패 - 세션ID: {}", session.getId(), e);
                    }
                })
                .count();

        log.info("브로드캐스트 메시지 전송 완료 - 사용자: {}명, 세션: {}개", userSessions.size(), totalSessions);
    }

    public void sendToGroup(String groupId, Object message) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.forEach(userId -> sendToUser(userId, message));
            log.info("그룹 메시지 전송 완료 - 그룹: {}명, 대상: {}명", groupId, members.size());
        }
    }

    public void sendToRoom(String roomId, Object message) {
        Set<String> members = roomMembers.get(roomId);
        if (members != null) {
            members.forEach(userId -> sendToUser(userId, message));
            log.info("채팅방 메시지 전송 완료 - 방: {}, 대상: {}명", roomId, members.size());
        }
    }

    public void broadcastRoomCreated(Map<String, String> roomCreatedEvent) {
        String json;
        try {
            json = objectMapper.writeValueAsString(roomCreatedEvent);
        } catch (Exception e) {
            log.error("채팅방 생성 이벤트 직렬화 실패", e);
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        long totalSessions = userSessions.values().stream()
                .flatMap(Set::stream)
                .filter(WebSocketSession::isOpen)
                .peek(session -> {
                    try {
                        synchronized (session) {
                            session.sendMessage(textMessage);
                        }
                    } catch (Exception e) {
                        log.error("채팅방 생성 이벤트 전송 실패 - 세션ID: {}", session.getId(), e);
                    }
                })
                .count();

        log.info("채팅방 생성 이벤트 브로드캐스트 완료 - 방ID: {}, 사용자: {}명, 세션: {}개",
                roomCreatedEvent.get("roomId"), userSessions.size(), totalSessions);
    }

    public void addUserToRoom(String userId, String roomId) {
        roomMembers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        log.info("사용자 채팅방 입장 - 사용자: {}. 방: {}", userId, roomId);
    }

    public void removeUserFromRoom(String userId, String roomId) {
        Set<String> members = roomMembers.get(roomId);
        if (members != null) {
            members.remove(userId);
            log.info("사용자 채팅방 퇴장 - 사용자: {}. 방: {}", userId, roomId);
        }
    }

    public void addUserToGroup(String userId, String groupId) {
       groupMembers.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        log.info("사용자 채팅방 입장 - 사용자: {}. 그룹: {}", userId, groupId);
    }

    public String getUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.substring(query.indexOf("userId=") + 7).split("&")[0];
        }
        return session.getId();
    }

    private void processClientMessage(String userId, String payload) {
        // Processing based on the message type sent by the client
        // ex: {"type": "join_room", "roomId": "room1"}
        // ex: {"type": "join_group", "groupId": "admin"}
        try {
            Map<String, Object> message = objectMapper.readValue(payload, Map.class);
            String type = (String) message.get("type");

            if (type == null) {
                log.warn("메시지 타입이 없습니다 - 사용자: {}, 페이로드: {}", userId, payload);
                return;
            }

            switch (type) {
                case "join_room":
                    String roomId = (String) message.get("roomId");
                    if (roomId != null) {
                        addUserToRoom(userId, roomId);
                        // send response of successful entry to chatroom
                        sendToUser(userId, Map.of(
                                "type", "room_joined",
                                "roomId", roomId,
                                "message", "채팅방에 입장했습니다"
                        ));
                    } else {
                        log.warn("roomId가 없습니다 - 사용자: {}", userId);
                    }
                    break;

                case "leave_room":
                    String leaveRoomId = (String) message.get("roomId");
                    if (leaveRoomId != null) {
                        removeUserFromRoom(userId, leaveRoomId);
                        // send response of successful leave to chatroom
                        sendToUser(userId, Map.of(
                                "type", "room_left",
                                "roomId", leaveRoomId,
                                "message", "채팅방에서 퇴장했습니다"
                        ));
                    } else {
                        log.warn("roomId가 없습니다 - 사용자: {}", userId);
                    }
                    break;

                case "join_group":
                    String groupId = (String) message.get("groupId");
                    if (groupId != null) {
                        addUserToGroup(userId, groupId);
                        // send response of successful entry to group
                        sendToUser(userId, Map.of(
                                "type", "group_joined",
                                "groupId", groupId,
                                "message", "그룹에 참여했습니다"
                        ));
                    } else {
                        log.warn("groupId가 없습니다 - 사용자: {}", userId);
                    }
                    break;

                case "ping":
                    // pong response about ping message, for confirming connection
                    sendToUser(userId, Map.of("type", "pong"));
                    break;

                default:
                    log.warn("알 수 없는 메시지 타입 - 사용자: {}, 타입: {}", userId, type);
                    break;
            }
        } catch (Exception e) {
            log.error("클라이언트 메시지 처리 실패 - 사용자: {}, 페이로드: {}", userId, payload, e);
        }
    }

    private void removeUserFromAllRooms(String userId) {
        roomMembers.values().forEach(members -> members.remove(userId));
        groupMembers.values().forEach(members -> members.remove(userId));
    }
}

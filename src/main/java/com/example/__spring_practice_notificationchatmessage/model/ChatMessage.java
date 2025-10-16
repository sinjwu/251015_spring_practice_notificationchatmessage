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
public class ChatMessage {
    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;

    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        SYSTEM,
        EMOJI,
        REPLY
    }

    public static ChatMessage textMessage(String roomId, String senderId, String senderName, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .messageType(MessageType.TEXT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ChatMessage userJoinedMessage(String roomId, String userId, String userName) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId("SYSTEM")
                .senderName("시스템")
                .content(userName + "님이 입장하셨습니다.")
                .messageType(MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of("userId", userId, "action", "join"))
                .build();
    }

    public static ChatMessage userLeftMessage(String roomId, String userId, String userName) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId("SYSTEM")
                .senderName("시스템")
                .content(userName + "님이 퇴장하셨습니다.")
                .messageType(MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of("userId", userId, "action", "leave"))
                .build();
    }

    public static ChatMessage systemAnnouncementMessage(String roomId, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId("SYSTEM")
                .senderName("시스템")
                .content(content)
                .messageType(MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of("action", "announcement"))
                .build();
    }

    public static ChatMessage imageMessage(String roomId, String senderId, String senderName, String imageUrl, String caption) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(caption != null ? caption: "")
                .messageType(MessageType.IMAGE)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of("imageUrl", imageUrl))
                .build();
    }

    public static ChatMessage fileMessage(String roomId, String senderId, String senderName, String fileName, String fileUrl, long fileSize) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(fileName)
                .messageType(MessageType.FILE)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of(
                        "fileName", fileName,
                        "fileUrl", fileUrl,
                        "fileSize", fileSize
                ))
                .build();
    }

    public static ChatMessage replyMessage(String roomId, String senderId, String senderName, String content, String replyToMessageId, String replyToContent) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .messageType(MessageType.REPLY)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of(
                        "replyToMessageId", replyToMessageId,
                        "replyToContent", replyToContent
                ))
                .build();
    }

    public static ChatMessage emojiMessage(String roomId, String senderId, String senderName, String emoji) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(emoji)
                .messageType(MessageType.EMOJI)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public boolean isSystemMessage() {return MessageType.SYSTEM.equals(messageType);}

    public boolean hasMedia() {return MessageType.IMAGE.equals(messageType) || MessageType.FILE.equals(messageType);}

    public boolean isReply() {return MessageType.REPLY.equals(messageType);}
}
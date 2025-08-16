package com.distri.chat.shared.dto;

import com.distri.chat.shared.enums.WebSocketEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * WebSocket标准消息格式
 * <p>
 * 设计原则：
 * 1. 事件驱动：event_type + event_data
 * 2. 可扩展：payload支持任意JSON结构
 * 3. 可追踪：messageId + timestamp
 * 4. 可靠性：ack机制支持
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 事件类型 - 核心字段，决定消息如何处理
     * 使用数字常量，减少传输带宽
     */
    @JsonProperty("event_type")
    private Integer eventType;

    /**
     * 事件数据 - 灵活的JSON对象，支持任意业务数据
     */
    @JsonProperty("event_data")
    private Object eventData;

    /**
     * 事件唯一ID - 用于ACK确认和去重
     */
    @JsonProperty("event_id")
    private String eventId;

    /**
     * 时间戳 - 消息创建时间
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * 是否需要ACK确认
     */
    @JsonProperty("ack_required")
    private Boolean ackRequired;

    /**
     * 扩展元数据 - 可选，用于业务扩展
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * 创建心跳消息
     */
    public static WebSocketMessage heartbeat(WebSocketEventType type) {
        WebSocketMessage message = new WebSocketMessage();
        message.setEventType(type.getValue());
        message.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        message.setAckRequired(false);
        return message;
    }

    /**
     * 创建业务消息
     */
    public static WebSocketMessage business(WebSocketEventType eventType, Object eventData) {
        WebSocketMessage message = new WebSocketMessage();
        message.setEventType(eventType.getValue());
        message.setEventData(eventData);
        message.setEventId(generateMessageId());
        message.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        message.setAckRequired(eventType.requiresAck());
        return message;
    }

    /**
     * 创建系统通知
     */
    public static WebSocketMessage notification(String message) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setEventType(WebSocketEventType.SYSTEM_NOTIFICATION.getValue());
        msg.setEventData(Map.of("message", message));
        msg.setEventId(generateMessageId());
        msg.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        msg.setAckRequired(false);
        return msg;
    }

    /**
     * 创建ACK响应
     */
    public static WebSocketMessage ack(String originalMessageId, boolean success, String error) {
        Map<String, Object> ackData = Map.of("original_message_id", originalMessageId, "success", success, "error", error != null ? error : "");

        WebSocketMessage message = new WebSocketMessage();
        message.setEventType(WebSocketEventType.SYSTEM_ACK.getValue());
        message.setEventData(ackData);
        message.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        message.setAckRequired(false);
        return message;
    }

    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
}
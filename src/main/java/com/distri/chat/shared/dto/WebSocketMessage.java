package com.distri.chat.shared.dto;

import com.distri.chat.shared.enums.WebSocketEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket标准消息格式
 * 
 * 设计原则：
 * 1. 事件驱动：event_type + event_data
 * 2. 可扩展：payload支持任意JSON结构
 * 3. 可追踪：messageId + timestamp
 * 4. 可靠性：ack机制支持
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
     * 消息唯一ID - 用于ACK确认和去重
     */
    @JsonProperty("message_id")
    private String messageId;
    
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
     * 消息来源 - 可选，用于路由和权限控制
     */
    @JsonProperty("source")
    private String source;
    
    /**
     * 消息目标 - 可选，用于定向推送
     */
    @JsonProperty("target")
    private String target;
    
    /**
     * 扩展元数据 - 可选，用于业务扩展
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // === 构造函数 ===
    
    public WebSocketMessage() {}
    
    public WebSocketMessage(Integer eventType, Object eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = LocalDateTime.now();
    }
    
    // === Getter/Setter方法 ===
    
    public Integer getEventType() { return eventType; }
    public void setEventType(Integer eventType) { this.eventType = eventType; }
    
    public Object getEventData() { return eventData; }
    public void setEventData(Object eventData) { this.eventData = eventData; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Boolean getAckRequired() { return ackRequired; }
    public void setAckRequired(Boolean ackRequired) { this.ackRequired = ackRequired; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // === 便捷构造方法 ===
    
    /**
     * 创建心跳消息
     */
    public static WebSocketMessage heartbeat(WebSocketEventType type) {
        WebSocketMessage message = new WebSocketMessage();
        message.setEventType(type.getValue());
        message.setTimestamp(LocalDateTime.now());
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
        message.setMessageId(generateMessageId());
        message.setTimestamp(LocalDateTime.now());
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
        msg.setMessageId(generateMessageId());
        msg.setTimestamp(LocalDateTime.now());
        msg.setAckRequired(false);
        return msg;
    }
    
    /**
     * 创建ACK响应
     */
    public static WebSocketMessage ack(String originalMessageId, boolean success, String error) {
        Map<String, Object> ackData = Map.of(
                "original_message_id", originalMessageId,
                "success", success,
                "error", error != null ? error : ""
        );
        
        WebSocketMessage message = new WebSocketMessage();
        message.setEventType(WebSocketEventType.SYSTEM_ACK.getValue());
        message.setEventData(ackData);
        message.setTimestamp(LocalDateTime.now());
        message.setAckRequired(false);
        return message;
    }
    
    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}
package com.distri.chat.shared.enums;

/**
 * WebSocket事件类型枚举
 * 
 * 优化设计：
 * 1. 使用数字常量，减少传输带宽（相比字符串节省60-80%）
 * 2. 分类编号，便于管理和扩展
 * 3. 前后端统一枚举，确保类型安全
 */
public enum WebSocketEventType {
    
    // === 系统级事件 (1-99) ===
    SYSTEM_HEARTBEAT_PING(1),
    SYSTEM_HEARTBEAT_PONG(2), 
    SYSTEM_ACK(3),
    SYSTEM_ERROR(4),
    SYSTEM_NOTIFICATION(5),
    
    // === 用户事件 (100-199) ===
    USER_ONLINE(100),
    USER_OFFLINE(101),
    USER_STATUS_CHANGE(102),
    USER_TYPING(103),
    
    // === 聊天事件 (200-299) ===
    CHAT_MESSAGE_SEND(200),
    CHAT_MESSAGE_RECEIVE(201),
    CHAT_MESSAGE_READ(202),
    CHAT_MESSAGE_RECALL(203),
    
    // === 群组事件 (300-399) ===
    GROUP_CREATE(300),
    GROUP_JOIN(301),
    GROUP_LEAVE(302),
    GROUP_MESSAGE(303),
    GROUP_MEMBER_CHANGE(304),
    
    // === 好友事件 (400-499) ===
    FRIEND_REQUEST(400),
    FRIEND_ACCEPT(401),
    FRIEND_REJECT(402),
    FRIEND_DELETE(403),
    
    // === 通话事件 (500-599) ===
    CALL_INVITE(500),
    CALL_ACCEPT(501),
    CALL_REJECT(502),
    CALL_END(503),
    
    // === 文件事件 (600-699) ===
    FILE_UPLOAD_START(600),
    FILE_UPLOAD_PROGRESS(601),
    FILE_UPLOAD_COMPLETE(602),
    FILE_DOWNLOAD(603);
    
    private final int value;
    
    WebSocketEventType(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    /**
     * 根据数字值获取枚举
     */
    public static WebSocketEventType fromValue(int value) {
        for (WebSocketEventType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为心跳事件
     */
    public boolean isHeartbeat() {
        return this == SYSTEM_HEARTBEAT_PING || this == SYSTEM_HEARTBEAT_PONG;
    }
    
    /**
     * 判断是否为系统事件
     */
    public boolean isSystem() {
        return value >= 1 && value <= 99;
    }
    
    /**
     * 判断是否为用户事件
     */
    public boolean isUser() {
        return value >= 100 && value <= 199;
    }
    
    /**
     * 判断是否为聊天事件
     */
    public boolean isChat() {
        return value >= 200 && value <= 299;
    }
    
    /**
     * 判断是否为群组事件
     */
    public boolean isGroup() {
        return value >= 300 && value <= 399;
    }
    
    /**
     * 判断是否需要ACK确认
     */
    public boolean requiresAck() {
        // 心跳和ACK本身不需要确认
        return !isHeartbeat() && this != SYSTEM_ACK && this != SYSTEM_ERROR;
    }
}

package com.distri.chat.infrastructure.websocket;

import com.distri.chat.shared.dto.WebSocketMessage;
import com.distri.chat.shared.enums.WebSocketEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * WebSocket消息处理器
 * 
 * 功能：
 * 1. 消息解析和路由
 * 2. 事件分发
 * 3. ACK处理
 * 4. 错误处理
 */
@Component
public class WebSocketMessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageProcessor.class);
    
    private final ObjectMapper objectMapper;
    private final WebSocketConnectionManager connectionManager;
    
    // 事件处理器映射
    private final Map<Integer, BiConsumer<String, WebSocketMessage>> eventHandlers = new HashMap<>();
    
    public WebSocketMessageProcessor(ObjectMapper objectMapper, WebSocketConnectionManager connectionManager) {
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
        initEventHandlers();
    }
    
    /**
     * 处理接收到的消息
     */
    public void processMessage(String channelId, String rawMessage) {
        try {
            // 统一解析JSON消息格式
            WebSocketMessage message = objectMapper.readValue(rawMessage, WebSocketMessage.class);
            
            // 重置心跳（收到任何消息都重置）
            connectionManager.resetHeartbeat(channelId);
            
            // 路由到对应的处理器
            Integer eventType = message.getEventType();
            BiConsumer<String, WebSocketMessage> handler = eventHandlers.get(eventType);
            
            if (handler != null) {
                handler.accept(channelId, message);
            } else {
                logger.warn("未知事件类型：{}，连接：{}", eventType, channelId);
                sendError(channelId, message.getMessageId(), "未知事件类型: " + eventType);
            }
            
        } catch (Exception e) {
            logger.error("处理消息失败，连接：{}，消息：{}", channelId, rawMessage, e);
            sendError(channelId, null, "消息格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 初始化事件处理器
     */
    private void initEventHandlers() {
        // 心跳事件
        eventHandlers.put(WebSocketEventType.SYSTEM_HEARTBEAT_PING.getValue(), this::handleHeartbeatPing);
        eventHandlers.put(WebSocketEventType.SYSTEM_HEARTBEAT_PONG.getValue(), this::handleHeartbeatPong);
        
        // ACK事件
        eventHandlers.put(WebSocketEventType.SYSTEM_ACK.getValue(), this::handleAck);
        
        // 用户事件
        eventHandlers.put(WebSocketEventType.USER_STATUS_CHANGE.getValue(), this::handleUserStatusChange);
        
        // 聊天事件
        eventHandlers.put(WebSocketEventType.CHAT_MESSAGE_SEND.getValue(), this::handleChatMessage);
        
        // TODO: 后续添加更多事件处理器
    }
    

    
    /**
     * 处理心跳ping
     */
    private void handleHeartbeatPing(String channelId, WebSocketMessage message) {
        logger.debug("收到心跳ping，连接：{}", channelId);
        
        // 回复pong
        WebSocketMessage pongMessage = WebSocketMessage.heartbeat(WebSocketEventType.SYSTEM_HEARTBEAT_PONG);
        sendMessage(channelId, pongMessage);
    }
    
    /**
     * 处理心跳pong
     */
    private void handleHeartbeatPong(String channelId, WebSocketMessage message) {
        logger.debug("收到心跳pong，连接：{}", channelId);
        connectionManager.handlePongReceived(channelId);
    }
    
    /**
     * 处理ACK确认
     */
    private void handleAck(String channelId, WebSocketMessage message) {
        logger.debug("收到ACK确认，连接：{}，消息ID：{}", channelId, message.getMessageId());
        // TODO: 实现ACK处理逻辑（如标记消息已送达）
    }
    
    /**
     * 处理用户状态变更
     */
    private void handleUserStatusChange(String channelId, WebSocketMessage message) {
        logger.info("收到用户状态变更，连接：{}，数据：{}", channelId, message.getEventData());
        
        // 发送ACK（如果需要）
        if (Boolean.TRUE.equals(message.getAckRequired())) {
            sendAck(channelId, message.getMessageId(), true, null);
        }
        
        // TODO: 实现状态变更业务逻辑
        // 1. 更新用户在线状态
        // 2. 通知好友
        // 3. 记录状态变更日志
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String channelId, WebSocketMessage message) {
        logger.info("收到聊天消息，连接：{}，数据：{}", channelId, message.getEventData());
        
        // 发送ACK（聊天消息需要确认）
        sendAck(channelId, message.getMessageId(), true, null);
        
        // TODO: 实现聊天消息业务逻辑
        // 1. 保存消息到数据库
        // 2. 推送给接收方
        // 3. 发送消息已送达通知
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(String channelId, WebSocketMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            connectionManager.sendMessage(channelId, jsonMessage);
        } catch (Exception e) {
            logger.error("发送消息失败，连接：{}", channelId, e);
        }
    }
    
    /**
     * 发送ACK确认
     */
    private void sendAck(String channelId, String originalMessageId, boolean success, String error) {
        WebSocketMessage ackMessage = WebSocketMessage.ack(originalMessageId, success, error);
        sendMessage(channelId, ackMessage);
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(String channelId, String originalMessageId, String error) {
        if (originalMessageId != null) {
            sendAck(channelId, originalMessageId, false, error);
        } else {
            WebSocketMessage errorMessage = new WebSocketMessage();
            errorMessage.setEventType(WebSocketEventType.SYSTEM_ERROR.getValue());
            errorMessage.setEventData(Map.of("error", error));
            sendMessage(channelId, errorMessage);
        }
    }
}

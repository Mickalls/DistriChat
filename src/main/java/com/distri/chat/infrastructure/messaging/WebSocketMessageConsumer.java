package com.distri.chat.infrastructure.messaging;

import com.distri.chat.infrastructure.websocket.WebSocketConnectionManager;
import com.distri.chat.infrastructure.websocket.WebSocketConnectionRegistry;
import com.distri.chat.shared.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * WebSocket消息推送的Kafka消费者
 * 负责消费针对当前服务器的WebSocket推送消息，并通过WebSocket连接推送给客户端
 */
@Component
public class WebSocketMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageConsumer.class);

    private final WebSocketConnectionManager connectionManager;
    private final WebSocketConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    public WebSocketMessageConsumer(WebSocketConnectionManager connectionManager,
                                  WebSocketConnectionRegistry connectionRegistry,
                                  ObjectMapper objectMapper) {
        this.connectionManager = connectionManager;
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * 消费WebSocket推送消息
     * Topic名称基于serverId动态生成：ws-push-{serverId}
     * 
     * @param message JSON格式的推送消息
     */
    @KafkaListener(
        topics = "#{@webSocketConnectionRegistry.serverId}",  // 动态Topic名称
        topicPattern = "ws-push-.*",                          // Topic模式匹配
        groupId = "#{@webSocketConnectionRegistry.serverId}-group"
    )
    public void consumeWebSocketMessage(String message) {
        try {
            logger.debug("收到WebSocket推送消息: {}", message);
            
            // 解析推送消息
            WebSocketPushMessage pushMessage = objectMapper.readValue(message, WebSocketPushMessage.class);
            
            // 验证消息是否发给当前服务器
            if (!connectionRegistry.getServerId().equals(pushMessage.getTargetServerId())) {
                logger.warn("收到非本服务器的推送消息: targetServerId={}, currentServerId={}", 
                        pushMessage.getTargetServerId(), connectionRegistry.getServerId());
                return;
            }
            
            // 处理推送消息
            processPushMessage(pushMessage);
            
        } catch (Exception e) {
            logger.error("处理WebSocket推送消息失败: message={}", message, e);
        }
    }

    /**
     * 处理推送消息
     */
    private void processPushMessage(WebSocketPushMessage pushMessage) {
        try {
            // 根据推送类型处理
            switch (pushMessage.getType()) {
                case SINGLE_USER_DEVICE:
                    // 推送给指定用户的指定设备
                    pushToUserDevice(pushMessage);
                    break;
                case SINGLE_USER_ALL_DEVICES:
                    // 推送给指定用户的所有设备
                    pushToUserAllDevices(pushMessage);
                    break;
                case BROADCAST:
                    // 广播给所有连接
                    broadcastMessage(pushMessage);
                    break;
                default:
                    logger.warn("未知的推送类型: {}", pushMessage.getType());
            }
        } catch (Exception e) {
            logger.error("处理推送消息失败: {}", pushMessage, e);
        }
    }

    /**
     * 推送给指定用户的指定设备
     */
    private void pushToUserDevice(WebSocketPushMessage pushMessage) {
        Long userId = pushMessage.getUserId();
        String deviceId = pushMessage.getDeviceId();
        WebSocketMessage wsMessage = pushMessage.getWebSocketMessage();
        
        // 构造channelId（需要与本地连接管理器的命名规则一致）
        String channelId = generateChannelId(userId, deviceId);
        
        // 发送消息
        boolean sent = connectionManager.sendMessage(channelId, wsMessage);
        
        logger.info("推送消息到用户设备: userId={}, deviceId={}, sent={}", userId, deviceId, sent);
    }

    /**
     * 推送给指定用户的所有设备
     */
    private void pushToUserAllDevices(WebSocketPushMessage pushMessage) {
        Long userId = pushMessage.getUserId();
        WebSocketMessage wsMessage = pushMessage.getWebSocketMessage();
        
        // 获取用户在当前服务器上的所有设备
        String currentServerId = connectionRegistry.getServerId();
        var devicesOnServer = connectionRegistry.getUserDevicesOnServer(userId, currentServerId);
        
        int sentCount = 0;
        for (String deviceId : devicesOnServer) {
            String channelId = generateChannelId(userId, deviceId);
            if (connectionManager.sendMessage(channelId, wsMessage)) {
                sentCount++;
            }
        }
        
        logger.info("推送消息到用户所有设备: userId={}, totalDevices={}, sentCount={}", 
                userId, devicesOnServer.size(), sentCount);
    }

    /**
     * 广播消息给所有连接
     */
    private void broadcastMessage(WebSocketPushMessage pushMessage) {
        WebSocketMessage wsMessage = pushMessage.getWebSocketMessage();
        
        connectionManager.broadcast(wsMessage);
        
        logger.info("广播消息: eventType={}", wsMessage.getEventType());
    }

    /**
     * 生成channelId
     * 需要与WebSocketHandler中的channelId生成规则保持一致
     */
    private String generateChannelId(Long userId, String deviceId) {
        // 这里的生成规则需要与实际的channelId命名保持一致
        // 如果是基于Netty的Channel ID，可能需要维护一个映射关系
        // 暂时使用简单的格式
        return String.format("user_%d_device_%s", userId, deviceId);
    }

    /**
     * WebSocket推送消息的数据结构
     */
    public static class WebSocketPushMessage {
        private PushType type;
        private String targetServerId;
        private Long userId;
        private String deviceId;
        private WebSocketMessage webSocketMessage;

        // 推送类型枚举
        public enum PushType {
            SINGLE_USER_DEVICE,    // 推送给指定用户的指定设备
            SINGLE_USER_ALL_DEVICES, // 推送给指定用户的所有设备
            BROADCAST              // 广播给所有连接
        }

        // Getters and Setters
        public PushType getType() { return type; }
        public void setType(PushType type) { this.type = type; }

        public String getTargetServerId() { return targetServerId; }
        public void setTargetServerId(String targetServerId) { this.targetServerId = targetServerId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public WebSocketMessage getWebSocketMessage() { return webSocketMessage; }
        public void setWebSocketMessage(WebSocketMessage webSocketMessage) { this.webSocketMessage = webSocketMessage; }

        @Override
        public String toString() {
            return "WebSocketPushMessage{" +
                    "type=" + type +
                    ", targetServerId='" + targetServerId + '\'' +
                    ", userId=" + userId +
                    ", deviceId='" + deviceId + '\'' +
                    ", webSocketMessage=" + webSocketMessage +
                    '}';
        }
    }
}

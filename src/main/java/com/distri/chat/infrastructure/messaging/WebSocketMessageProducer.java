package com.distri.chat.infrastructure.messaging;

import com.distri.chat.infrastructure.websocket.WebSocketConnectionRegistry;
import com.distri.chat.shared.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket消息推送的Kafka生产者
 * 负责向其他WebSocket服务器发送推送消息
 */
@Component
public class WebSocketMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebSocketConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;

    // Topic前缀
    private static final String TOPIC_PREFIX = "ws-push-";

    public WebSocketMessageProducer(KafkaTemplate<String, String> kafkaTemplate,
                                  WebSocketConnectionRegistry connectionRegistry,
                                  ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * 推送消息给指定用户的指定设备
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param message WebSocket消息
     */
    public void pushToUserDevice(Long userId, String deviceId, WebSocketMessage message) {
        try {
            // 查找设备对应的服务器
            String serverId = connectionRegistry.findServerByUserDevice(userId, deviceId);
            
            if (serverId == null) {
                logger.warn("用户设备未在线: userId={}, deviceId={}", userId, deviceId);
                return;
            }
            
            // 构造推送消息
            WebSocketMessageConsumer.WebSocketPushMessage pushMessage = new WebSocketMessageConsumer.WebSocketPushMessage();
            pushMessage.setType(WebSocketMessageConsumer.WebSocketPushMessage.PushType.SINGLE_USER_DEVICE);
            pushMessage.setTargetServerId(serverId);
            pushMessage.setUserId(userId);
            pushMessage.setDeviceId(deviceId);
            pushMessage.setWebSocketMessage(message);
            
            // 发送到对应服务器的Topic
            sendToServer(serverId, pushMessage);
            
            logger.info("推送消息到用户设备: userId={}, deviceId={}, serverId={}", userId, deviceId, serverId);
            
        } catch (Exception e) {
            logger.error("推送消息到用户设备失败: userId={}, deviceId={}", userId, deviceId, e);
        }
    }

    /**
     * 推送消息给指定用户的所有设备
     * 
     * @param userId 用户ID
     * @param message WebSocket消息
     */
    public void pushToUserAllDevices(Long userId, WebSocketMessage message) {
        try {
            // 获取用户的所有在线设备
            Set<String> devices = connectionRegistry.getUserDevices(userId);
            
            if (devices.isEmpty()) {
                logger.warn("用户无在线设备: userId={}", userId);
                return;
            }
            
            // 按服务器分组发送
            Set<String> sentServers = new HashSet<>();
            for (String deviceId : devices) {
                String serverId = connectionRegistry.findServerByUserDevice(userId, deviceId);
                
                if (serverId != null && !sentServers.contains(serverId)) {
                    // 构造推送消息
                    WebSocketMessageConsumer.WebSocketPushMessage pushMessage = new WebSocketMessageConsumer.WebSocketPushMessage();
                    pushMessage.setType(WebSocketMessageConsumer.WebSocketPushMessage.PushType.SINGLE_USER_ALL_DEVICES);
                    pushMessage.setTargetServerId(serverId);
                    pushMessage.setUserId(userId);
                    pushMessage.setWebSocketMessage(message);
                    
                    // 发送到对应服务器的Topic
                    sendToServer(serverId, pushMessage);
                    sentServers.add(serverId);
                }
            }
            
            logger.info("推送消息到用户所有设备: userId={}, devices={}, servers={}", 
                    userId, devices.size(), sentServers.size());
            
        } catch (Exception e) {
            logger.error("推送消息到用户所有设备失败: userId={}", userId, e);
        }
    }

    /**
     * 广播消息给所有WebSocket服务器
     * 
     * @param message WebSocket消息
     */
    public void broadcastToAllServers(WebSocketMessage message) {
        try {
            // 获取所有在线的WebSocket服务器
            Set<String> onlineServers = connectionRegistry.getAllOnlineServers();
            
            if (onlineServers.isEmpty()) {
                logger.warn("没有在线的WebSocket服务器");
                return;
            }
            
            // 发送到所有服务器
            for (String serverId : onlineServers) {
                // 构造推送消息
                WebSocketMessageConsumer.WebSocketPushMessage pushMessage = new WebSocketMessageConsumer.WebSocketPushMessage();
                pushMessage.setType(WebSocketMessageConsumer.WebSocketPushMessage.PushType.BROADCAST);
                pushMessage.setTargetServerId(serverId);
                pushMessage.setWebSocketMessage(message);
                
                // 发送到对应服务器的Topic
                sendToServer(serverId, pushMessage);
            }
            
            logger.info("广播消息到所有服务器: servers={}, eventType={}", 
                    onlineServers.size(), message.getEventType());
            
        } catch (Exception e) {
            logger.error("广播消息到所有服务器失败", e);
        }
    }

    /**
     * 发送消息到指定服务器的Topic
     */
    private void sendToServer(String serverId, WebSocketMessageConsumer.WebSocketPushMessage pushMessage) {
        try {
            String topic = TOPIC_PREFIX + serverId;
            String messageJson = objectMapper.writeValueAsString(pushMessage);
            
            kafkaTemplate.send(topic, messageJson);
            
            logger.debug("已发送推送消息到Kafka: topic={}, message={}", topic, messageJson);
            
        } catch (Exception e) {
            logger.error("发送推送消息到Kafka失败: serverId={}, message={}", serverId, pushMessage, e);
        }
    }

    /**
     * 检查用户是否在线（有任何设备在线）
     * 
     * @param userId 用户ID
     * @return true-在线, false-离线
     */
    public boolean isUserOnline(Long userId) {
        Set<String> devices = connectionRegistry.getUserDevices(userId);
        return !devices.isEmpty();
    }

    /**
     * 检查用户指定设备是否在线
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return true-在线, false-离线
     */
    public boolean isUserDeviceOnline(Long userId, String deviceId) {
        String serverId = connectionRegistry.findServerByUserDevice(userId, deviceId);
        return serverId != null;
    }

    /**
     * 获取用户的在线设备数量
     * 
     * @param userId 用户ID
     * @return 在线设备数量
     */
    public int getUserOnlineDeviceCount(Long userId) {
        Set<String> devices = connectionRegistry.getUserDevices(userId);
        return devices.size();
    }
}

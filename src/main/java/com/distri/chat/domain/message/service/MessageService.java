package com.distri.chat.domain.message.service;

import com.distri.chat.infra.messaging.WebSocketMessageProducer;
import com.distri.chat.shared.dto.MessageDto;
import com.distri.chat.shared.dto.WebSocketMessage;
import com.distri.chat.shared.enums.WebSocketEventType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

/**
 * MessageService 处理各种类型的消息
 * 构造 WebSocketMessage (生成消息ID)
 * 并推送消息
 * todo: 考虑作为通用service, 支持热插拔中间件, 如事物总线支持kafka, rocketmq, rabbitmq, redis...
 */
@Service
public class MessageService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketMessageProducer webSocketMessageProducer;

    public MessageService(RedisTemplate<String, Object> redisTemplate,
                          WebSocketMessageProducer webSocketMessageProducer) {
        this.redisTemplate = redisTemplate;
        this.webSocketMessageProducer = webSocketMessageProducer;
    }

    /**
     * 消息推送函数
     * 接受消息DTO, 转换为WebSocketMessage并推送消息
     */
    public void publishMsg(MessageDto dto) {
        WebSocketMessage msg = buildWebsocketMsg(dto);

        // 发送 mq 消息
        webSocketMessageProducer.pushToUserAllDevices(Long.valueOf(dto.getToUserId()), msg);
    }

    private WebSocketMessage buildWebsocketMsg(MessageDto dto) {
        return WebSocketMessage.builder()
                .eventType(WebSocketEventType.CHAT_MESSAGE_SEND.getValue())
                .eventData(dto)
                .eventId(null)
                .timestamp(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
                .ackRequired(false)
                .metadata(new HashMap<>())
                .build();
    }

    public String generateMessageId(String fromUserId) {
        // userId 维度 redis incr 并转为 String
        return String.valueOf(redisTemplate.opsForValue().increment("msg_id:" + fromUserId));
    }
}

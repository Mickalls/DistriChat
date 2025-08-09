package com.distri.chat.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka配置类
 * Kafka为必需组件，必须连接成功
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    public KafkaConfig() {
        logger.info("Kafka配置已加载，开始初始化必需的Topics");
    }

    /**
     * 消息主题
     */
    @Bean
    public NewTopic messageTopic() {
        return TopicBuilder.name("distri-chat-message")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 用户事件主题
     */
    @Bean
    public NewTopic userEventTopic() {
        return TopicBuilder.name("distri-chat-user-event")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

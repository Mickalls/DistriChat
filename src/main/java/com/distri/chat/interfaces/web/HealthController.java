package com.distri.chat.interfaces.web;

import com.distri.chat.infrastructure.websocket.WebSocketConnectionManager;
import com.distri.chat.shared.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查Controller - 用于测试各个组件是否正常工作
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "健康检查", description = "系统健康状态检查接口")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired 
    private WebSocketConnectionManager connectionManager;

    @Operation(summary = "检查系统健康状态", description = "检查数据库、Redis、Kafka、WebSocket等组件的连接状态")
    @GetMapping("/check")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("status", "UP");

        // 检查数据库连接
        healthData.put("database", checkDatabase());

        // 检查Redis连接
        healthData.put("redis", checkRedis());

        // 检查Kafka连接
        healthData.put("kafka", checkKafka());

        // 检查WebSocket状态
        healthData.put("websocket", checkWebSocket());

        return Result.success("系统健康检查完成", healthData);
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbStatus = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            dbStatus.put("status", connection.isValid(5) ? "UP" : "DOWN");
            dbStatus.put("url", connection.getMetaData().getURL());
        } catch (Exception e) {
            dbStatus.put("status", "DOWN");
            dbStatus.put("error", e.getMessage());
        }
        return dbStatus;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> redisStatus = new HashMap<>();
        try {
            String testKey = "health:check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test");
            String value = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            redisStatus.put("status", "test".equals(value) ? "UP" : "DOWN");
            if ("test".equals(value)) {
                redisStatus.put("message", "Redis连接正常");
            }
        } catch (Exception e) {
            redisStatus.put("status", "DOWN");
            redisStatus.put("error", e.getMessage());
            redisStatus.put("errorClass", e.getClass().getSimpleName());
        }
        return redisStatus;
    }

    private Map<String, Object> checkKafka() {
        Map<String, Object> kafkaStatus = new HashMap<>();
        try {
            // 尝试发送测试消息
            kafkaTemplate.send("distri-chat-message", "health-check", "test message");
            kafkaStatus.put("status", "UP");
            kafkaStatus.put("message", "Kafka连接正常");
        } catch (Exception e) {
            kafkaStatus.put("status", "DOWN");
            kafkaStatus.put("error", e.getMessage());
        }
        return kafkaStatus;
    }

    private Map<String, Object> checkWebSocket() {
        Map<String, Object> wsStatus = new HashMap<>();
        try {
            var stats = connectionManager.getStats();
            wsStatus.put("status", "UP");
            wsStatus.put("activeConnections", stats.getActiveConnections());
            wsStatus.put("totalConnections", stats.getTotalConnections());
            wsStatus.put("heartbeatSent", stats.getHeartbeatSentCount());
            wsStatus.put("heartbeatTimeout", stats.getHeartbeatTimeoutCount());
        } catch (Exception e) {
            wsStatus.put("status", "DOWN");
            wsStatus.put("error", e.getMessage());
        }
        return wsStatus;
    }

    @Operation(summary = "测试WebSocket广播", description = "向所有WebSocket连接广播测试消息")
    @GetMapping("/websocket-test")
    public Result<Map<String, Object>> testWebSocket() {
        String message = "测试广播消息 - " + LocalDateTime.now();
        connectionManager.broadcast(message);
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "广播消息已发送");
        data.put("content", message);
        data.put("activeConnections", connectionManager.getStats().getActiveConnections());
        
        return Result.success("WebSocket广播测试成功", data);
    }
}

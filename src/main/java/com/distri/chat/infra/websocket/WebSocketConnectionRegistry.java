package com.distri.chat.infra.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WebSocket连接注册中心
 * 负责在Redis中管理WebSocket连接信息，用于消息推送时的连接定位
 */
@Component
@Slf4j
public class WebSocketConnectionRegistry {
    // Redis键设计
    private static final String REDIS_KEY_PREFIX = "distri_chat:ws:";

    // [Redis Hash] 用户连接信息：user_connections:{userId} = {deviceId: serverId, ...}
    private static final String USER_CONNECTIONS_KEY_PREFIX = REDIS_KEY_PREFIX + "user_connections:";
    // 服务器注册：servers:{serverId} = {host:port:wsPort:startTime}
    private static final String SERVER_REGISTRY_KEY = REDIS_KEY_PREFIX + "servers:";
    private final RedisTemplate<String, String> redisTemplate;
    // 服务器唯一标识
    private final String serverId;
    @Value("${server.port:28080}")
    private int serverPort;
    @Value("${websocket.port:9000}")
    private int websocketPort;

    public WebSocketConnectionRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serverId = generateServerId();
    }

    private String getUserConnectionsKey(String userId) {
        return USER_CONNECTIONS_KEY_PREFIX + userId;
    }

    /**
     * 生成服务器唯一标识
     */
    private String generateServerId() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return hostName + "_" + serverPort + "_" + System.currentTimeMillis();
        } catch (Exception e) {
            return "server_" + serverPort + "_" + System.currentTimeMillis();
        }
    }

    /**
     * 服务器启动时注册服务器信息
     */
    @PostConstruct
    public void registerServer() {
        try {
            String serverKey = SERVER_REGISTRY_KEY + serverId;
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String serverInfo = String.format("%s:%d:%d:%d", hostAddress, serverPort, websocketPort, System.currentTimeMillis());

            // 注册服务器信息，TTL为2小时
            redisTemplate.opsForValue().set(serverKey, serverInfo, 2, TimeUnit.HOURS);

            log.info("WebSocket服务器已注册到Redis: serverId={}, info={}", serverId, serverInfo);
        } catch (Exception e) {
            log.error("注册WebSocket服务器失败", e);
        }
    }

    /**
     * 用户WebSocket连接建立时注册连接信息
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     */
    public void registerUserConnection(Long userId, String deviceId) {
        try {
            // 1. 构建key = prefix:{user_id}, val = field-deviceId, value-serverId
            String userConnectionsKey = getUserConnectionsKey(userId.toString());

            redisTemplate.opsForHash().put(userConnectionsKey, deviceId, serverId);

            redisTemplate.expire(userConnectionsKey, 1, TimeUnit.HOURS);

            log.info("用户连接已注册到Redis: userId={}, deviceId={}, serverId={}", userId, deviceId, serverId);
        } catch (Exception e) {
            log.error("注册用户连接失败: userId={}, deviceId={}", userId, deviceId, e);
        }
    }

    /**
     * 用户WebSocket连接断开时注销连接信息
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     */
    public void unregisterUserConnection(Long userId, String deviceId) {
        try {
            String userConnectionsKey = getUserConnectionsKey(userId.toString());

            redisTemplate.opsForHash().delete(userConnectionsKey, deviceId);
            if (redisTemplate.opsForHash().size(userConnectionsKey) == 0) {
                redisTemplate.delete(userConnectionsKey);
            }

            log.info("用户连接已从Redis中删除: userId={}, deviceId={}", userId, deviceId);
        } catch (Exception e) {
            log.error("注销用户连接失败: userId={}, deviceId={}", userId, deviceId, e);
        }
    }

    public Map<String, Boolean> batchGetUserOnlineStatus(List<String> userIds) {
        Map<String, Boolean> result = new HashMap<>();

        try {
            List<Object> pipelineResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String userId : userIds) {
                    String userConnectionsKey = getUserConnectionsKey(userId);
                    connection.hLen(userConnectionsKey.getBytes());
                }
                return null;
            });
            for (int i = 0; i < userIds.size(); i++) {
                String userId = userIds.get(i);
                Object fieldCountObj = pipelineResults.get(i);
                boolean isOnline = fieldCountObj != null && (Long) fieldCountObj > 0;
                result.put(userId, isOnline);
            }
        } catch (Exception e) {
            log.error("批量查询用户在线状态失败: userIds={}", userIds, e);
        }

        return result;
    }

    /**
     * 根据用户ID和设备ID查找对应的服务器ID
     *
     * @param userId   用户ID
     * @param deviceId 设备ID
     * @return 服务器ID，如果未找到返回null
     */
    public String findServerByUserDevice(Long userId, String deviceId) {
        try {
            String userConnectionsKey = getUserConnectionsKey(userId.toString());
            String serverId = (String) redisTemplate.opsForHash().get(userConnectionsKey, deviceId);

            log.debug("查找用户设备对应服务器: userId={}, deviceId={}, serverId={}", userId, deviceId, serverId);
            return serverId;
        } catch (Exception e) {
            log.error("查找用户设备对应服务器失败: userId={}, deviceId={}", userId, deviceId, e);
            return null;
        }
    }

    /**
     * 获取用户的所有在线设备ID列表
     *
     * @param userId 用户ID
     * @return 设备ID列表
     */
    public Set<String> getUserDevices(Long userId) {
        try {
            String userConnectionsKey = getUserConnectionsKey(userId.toString());
            // 拿 Redis Hash 的所有 Fields 并流式转换为 devices
            Set<String> devices = redisTemplate.opsForHash().keys(userConnectionsKey).stream().map(Object::toString).collect(Collectors.toSet());

            log.debug("获取用户在线设备: userId={}, devices={}", userId, devices);
            return devices;
        } catch (Exception e) {
            log.error("获取用户在线设备失败: userId={}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * 获取用户在指定服务器上的设备列表
     *
     * @param userId         用户ID
     * @param targetServerId 目标服务器ID
     * @return 在指定服务器上的设备ID列表
     */
    public Set<String> getUserDevicesOnServer(Long userId, String targetServerId) {
        Set<String> devicesOnServer = new HashSet<>();
        Set<String> allDevices = getUserDevices(userId);

        for (String deviceId : allDevices) {
            String serverId = findServerByUserDevice(userId, deviceId);
            if (targetServerId.equals(serverId)) {
                devicesOnServer.add(deviceId);
            }
        }

        log.debug("获取用户在服务器上的设备: userId={}, serverId={}, devices={}", userId, targetServerId, devicesOnServer);
        return devicesOnServer;
    }

    /**
     * 获取当前服务器ID
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * 获取服务器信息
     *
     * @param serverId 服务器ID
     * @return 服务器信息字符串 (host:port:wsPort:startTime)
     */
    public String getServerInfo(String serverId) {
        try {
            String serverKey = SERVER_REGISTRY_KEY + serverId;
            return redisTemplate.opsForValue().get(serverKey);
        } catch (Exception e) {
            log.error("获取服务器信息失败: serverId={}", serverId, e);
            return null;
        }
    }

    /**
     * 获取所有在线的WebSocket服务器ID列表
     *
     * @return 服务器ID列表
     */
    public Set<String> getAllOnlineServers() {
        try {
            Set<String> serverKeys = redisTemplate.keys(SERVER_REGISTRY_KEY + "*");
            Set<String> serverIds = new HashSet<>();

            if (serverKeys != null) {
                for (String key : serverKeys) {
                    String serverId = key.substring(SERVER_REGISTRY_KEY.length());
                    serverIds.add(serverId);
                }
            }

            log.debug("获取所有在线服务器: serverIds={}", serverIds);
            return serverIds;
        } catch (Exception e) {
            log.error("获取所有在线服务器失败", e);
            return new HashSet<>();
        }
    }

    /**
     * 服务器关闭时注销服务器信息
     */
    @PreDestroy
    public void unregisterServer() {
        try {
            String serverKey = SERVER_REGISTRY_KEY + serverId;
            redisTemplate.delete(serverKey);

            log.info("WebSocket服务器已从Redis中注销: serverId={}", serverId);
        } catch (Exception e) {
            log.error("注销WebSocket服务器失败", e);
        }
    }
}

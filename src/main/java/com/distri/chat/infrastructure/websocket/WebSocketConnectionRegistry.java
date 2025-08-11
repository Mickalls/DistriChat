package com.distri.chat.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket连接注册中心
 * 负责在Redis中管理WebSocket连接信息，用于消息推送时的连接定位
 */
@Component
public class WebSocketConnectionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConnectionRegistry.class);

    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${server.port:28080}")
    private int serverPort;
    
    @Value("${websocket.port:9000}")
    private int websocketPort;
    
    // 服务器唯一标识
    private final String serverId;
    
    // Redis键设计
    private static final String REDIS_KEY_PREFIX = "distri_chat:ws:";
    
    // 服务器注册：servers:{serverId} = {host:port:wsPort:startTime}
    private static final String SERVER_REGISTRY_KEY = REDIS_KEY_PREFIX + "servers:";
    
    // 用户设备连接映射：user_device:{userId}:{deviceId} = {serverId}
    private static final String USER_DEVICE_KEY = REDIS_KEY_PREFIX + "user_device:";
    
    // 用户所有设备列表：user_devices:{userId} = [deviceId1, deviceId2, ...]
    private static final String USER_DEVICES_KEY = REDIS_KEY_PREFIX + "user_devices:";

    public WebSocketConnectionRegistry(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serverId = generateServerId();
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
            String serverInfo = String.format("%s:%d:%d:%d", 
                    hostAddress, serverPort, websocketPort, System.currentTimeMillis());
            
            // 注册服务器信息，TTL为2小时
            redisTemplate.opsForValue().set(serverKey, serverInfo, 2, TimeUnit.HOURS);
            
            logger.info("WebSocket服务器已注册到Redis: serverId={}, info={}", serverId, serverInfo);
        } catch (Exception e) {
            logger.error("注册WebSocket服务器失败", e);
        }
    }

    /**
     * 用户WebSocket连接建立时注册连接信息
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    public void registerUserConnection(Long userId, String deviceId) {
        try {
            // 1. 注册用户设备到服务器的映射
            String userDeviceKey = USER_DEVICE_KEY + userId + ":" + deviceId;
            redisTemplate.opsForValue().set(userDeviceKey, serverId, 1, TimeUnit.HOURS);
            
            // 2. 将设备添加到用户设备列表中
            String userDevicesKey = USER_DEVICES_KEY + userId;
            redisTemplate.opsForSet().add(userDevicesKey, deviceId);
            redisTemplate.expire(userDevicesKey, 1, TimeUnit.HOURS);
            
            logger.info("用户连接已注册到Redis: userId={}, deviceId={}, serverId={}", 
                    userId, deviceId, serverId);
        } catch (Exception e) {
            logger.error("注册用户连接失败: userId={}, deviceId={}", userId, deviceId, e);
        }
    }

    /**
     * 用户WebSocket连接断开时注销连接信息
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    public void unregisterUserConnection(Long userId, String deviceId) {
        try {
            // 1. 删除用户设备到服务器的映射
            String userDeviceKey = USER_DEVICE_KEY + userId + ":" + deviceId;
            redisTemplate.delete(userDeviceKey);
            
            // 2. 从用户设备列表中移除设备
            String userDevicesKey = USER_DEVICES_KEY + userId;
            redisTemplate.opsForSet().remove(userDevicesKey, deviceId);
            
            logger.info("用户连接已从Redis中删除: userId={}, deviceId={}", userId, deviceId);
        } catch (Exception e) {
            logger.error("注销用户连接失败: userId={}, deviceId={}", userId, deviceId, e);
        }
    }

    /**
     * 根据用户ID和设备ID查找对应的服务器ID
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return 服务器ID，如果未找到返回null
     */
    public String findServerByUserDevice(Long userId, String deviceId) {
        try {
            String userDeviceKey = USER_DEVICE_KEY + userId + ":" + deviceId;
            String serverId = redisTemplate.opsForValue().get(userDeviceKey);
            
            logger.debug("查找用户设备对应服务器: userId={}, deviceId={}, serverId={}", 
                    userId, deviceId, serverId);
            return serverId;
        } catch (Exception e) {
            logger.error("查找用户设备对应服务器失败: userId={}, deviceId={}", userId, deviceId, e);
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
            String userDevicesKey = USER_DEVICES_KEY + userId;
            Set<String> devices = redisTemplate.opsForSet().members(userDevicesKey);
            
            logger.debug("获取用户在线设备: userId={}, devices={}", userId, devices);
            return devices != null ? devices : new HashSet<>();
        } catch (Exception e) {
            logger.error("获取用户在线设备失败: userId={}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * 获取用户在指定服务器上的设备列表
     * 
     * @param userId 用户ID
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
        
        logger.debug("获取用户在服务器上的设备: userId={}, serverId={}, devices={}", 
                userId, targetServerId, devicesOnServer);
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
            logger.error("获取服务器信息失败: serverId={}", serverId, e);
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
            
            logger.debug("获取所有在线服务器: serverIds={}", serverIds);
            return serverIds;
        } catch (Exception e) {
            logger.error("获取所有在线服务器失败", e);
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
            
            logger.info("WebSocket服务器已从Redis中注销: serverId={}", serverId);
        } catch (Exception e) {
            logger.error("注销WebSocket服务器失败", e);
        }
    }
}

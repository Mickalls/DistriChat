package com.distri.chat.infrastructure.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket连接管理器
 * 统一管理所有WebSocket连接、心跳检测、统计信息
 */
@Component
public class WebSocketConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConnectionManager.class);

    // 连接存储
    private final ConcurrentMap<String, ChannelHandlerContext> connections = new ConcurrentHashMap<>();
    
    // 心跳相关
    private final ConcurrentMap<String, Timeout> heartbeatTimeouts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timeout> pongTimeouts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> waitingPong = new ConcurrentHashMap<>();
    
    // 时间轮定时器
    private final HashedWheelTimer timer = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 512);
    
    // 统计信息
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong heartbeatSentCount = new AtomicLong(0);
    private final AtomicLong heartbeatTimeoutCount = new AtomicLong(0);

    /**
     * 添加连接
     */
    public void addConnection(String channelId, ChannelHandlerContext ctx) {
        connections.put(channelId, ctx);
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
        
        // 启动心跳检测
        startHeartbeat(channelId, ctx);
        
        logger.info("WebSocket连接建立：{}，当前活跃连接数：{}", channelId, activeConnections.get());
    }

    /**
     * 移除连接
     */
    public void removeConnection(String channelId) {
        if (connections.remove(channelId) != null) {
            activeConnections.decrementAndGet();
            
            // 清理心跳相关数据
            clearHeartbeatData(channelId);
            
            logger.info("WebSocket连接断开：{}，当前活跃连接数：{}", channelId, activeConnections.get());
        }
    }

    /**
     * 获取连接
     */
    public ChannelHandlerContext getConnection(String channelId) {
        return connections.get(channelId);
    }

    /**
     * 检查连接是否存在
     */
    public boolean hasConnection(String channelId) {
        return connections.containsKey(channelId);
    }

    /**
     * 清理心跳相关数据
     */
    private void clearHeartbeatData(String channelId) {
        // 清理心跳定时任务
        Timeout heartbeatTimeout = heartbeatTimeouts.remove(channelId);
        if (heartbeatTimeout != null && !heartbeatTimeout.isCancelled()) {
            heartbeatTimeout.cancel();
        }
        
        // 清理pong等待任务
        Timeout pongTimeout = pongTimeouts.remove(channelId);
        if (pongTimeout != null && !pongTimeout.isCancelled()) {
            pongTimeout.cancel();
        }
        
        // 清理等待状态
        waitingPong.remove(channelId);
    }

    /**
     * 设置心跳检测定时器
     */
    public void setHeartbeatTimer(String channelId, ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        // 清理旧的定时器
        Timeout oldTimeout = heartbeatTimeouts.remove(channelId);
        if (oldTimeout != null && !oldTimeout.isCancelled()) {
            oldTimeout.cancel();
        }
        
        // 设置新的定时器
        Timeout newTimeout = timer.newTimeout(timeout -> {
            if (ctx.channel().isActive()) {
                task.run();
            }
        }, delay, unit);
        
        heartbeatTimeouts.put(channelId, newTimeout);
    }

    /**
     * 设置pong等待定时器
     */
    public void setPongTimer(String channelId, Runnable task, long delay, TimeUnit unit) {
        Timeout pongTimeout = timer.newTimeout(timeout -> task.run(), delay, unit);
        pongTimeouts.put(channelId, pongTimeout);
    }

    /**
     * 取消pong等待定时器
     */
    public void cancelPongTimer(String channelId) {
        Timeout pongTimeout = pongTimeouts.remove(channelId);
        if (pongTimeout != null && !pongTimeout.isCancelled()) {
            pongTimeout.cancel();
        }
    }

    /**
     * 设置等待pong状态
     */
    public void setWaitingPong(String channelId, int retryCount) {
        waitingPong.put(channelId, retryCount);
    }

    /**
     * 获取等待pong状态
     */
    public Integer getWaitingPong(String channelId) {
        return waitingPong.get(channelId);
    }

    /**
     * 清除等待pong状态
     */
    public void clearWaitingPong(String channelId) {
        waitingPong.remove(channelId);
    }

    /**
     * 增加心跳发送计数
     */
    public void incrementHeartbeatSent() {
        heartbeatSentCount.incrementAndGet();
    }

    /**
     * 增加心跳超时计数
     */
    public void incrementHeartbeatTimeout() {
        heartbeatTimeoutCount.incrementAndGet();
    }

    /**
     * 获取连接统计信息
     */
    public ConnectionStats getStats() {
        return new ConnectionStats(
            totalConnections.get(),
            activeConnections.get(),
            heartbeatSentCount.get(),
            heartbeatTimeoutCount.get()
        );
    }

    /**
     * 向指定连接发送消息
     */
    public boolean sendMessage(String channelId, String message) {
        ChannelHandlerContext ctx = connections.get(channelId);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(message));
            return true;
        }
        return false;
    }

    /**
     * 广播消息给所有连接
     */
    public void broadcast(String message) {
        connections.values().forEach(ctx -> {
            if (ctx.channel().isActive()) {
                ctx.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(message));
            }
        });
    }

    /**
     * 启动心跳检测
     */
    private void startHeartbeat(String channelId, ChannelHandlerContext ctx) {
        setHeartbeatTimer(channelId, ctx, () -> handleReaderIdle(channelId), 75, TimeUnit.SECONDS);
    }

    /**
     * 重置心跳检测
     */
    public void resetHeartbeat(String channelId) {
        ChannelHandlerContext ctx = connections.get(channelId);
        if (ctx != null) {
            // 清理所有相关定时器和状态
            clearHeartbeatData(channelId);
            // 重新启动心跳检测
            startHeartbeat(channelId, ctx);
        }
    }

    /**
     * 处理读超时 - IdleStateHandler触发
     */
    public void handleReaderIdle(String channelId) {
        ChannelHandlerContext ctx = connections.get(channelId);
        if (ctx == null || !ctx.channel().isActive()) {
            return;
        }

        Integer retryCount = waitingPong.get(channelId);
        
        if (retryCount == null) {
            // 第一次发送ping
            logger.debug("75秒无消息，发送ping检测连接：{}", channelId);
            waitingPong.put(channelId, 1);
            ctx.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame("ping"));
            incrementHeartbeatSent();
            
            // 15秒后检查pong响应
            setPongTimer(channelId, () -> checkPongResponse(channelId), 15, TimeUnit.SECONDS);
            
        } else if (retryCount < 2) {
            // 第二次发送ping
            logger.warn("第{}次ping检测，连接：{}", retryCount + 1, channelId);
            waitingPong.put(channelId, retryCount + 1);
            ctx.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame("ping"));
            incrementHeartbeatSent();
            
            // 15秒后再次检查
            setPongTimer(channelId, () -> checkPongResponse(channelId), 15, TimeUnit.SECONDS);
            
        } else {
            // 超过重试次数，关闭连接
            logger.warn("ping检测失败，关闭连接：{}", channelId);
            incrementHeartbeatTimeout();
            ctx.close();
        }
    }

    /**
     * 处理收到pong响应
     */
    public void handlePongReceived(String channelId) {
        // 取消pong等待定时器
        cancelPongTimer(channelId);
        // 清除等待状态
        clearWaitingPong(channelId);
        // 重置心跳检测
        resetHeartbeat(channelId);
    }

    /**
     * 检查pong响应超时
     */
    private void checkPongResponse(String channelId) {
        ChannelHandlerContext ctx = connections.get(channelId);
        if (ctx == null || !ctx.channel().isActive()) {
            return;
        }

        if (waitingPong.containsKey(channelId)) {
            // 仍在等待pong，说明客户端无响应
            Integer retryCount = waitingPong.get(channelId);
            if (retryCount >= 2) {
                logger.warn("连续{}次ping无响应，关闭连接：{}", retryCount, channelId);
                incrementHeartbeatTimeout();
                ctx.close();
            } else {
                // 继续下一轮ping检测
                handleReaderIdle(channelId);
            }
        }
    }

    /**
     * 优雅关闭定时器
     */
    @PreDestroy
    public void shutdown() {
        logger.info("正在关闭WebSocket连接管理器...");
        
        // 清理所有连接
        connections.clear();
        heartbeatTimeouts.clear();
        pongTimeouts.clear();
        waitingPong.clear();
        
        // 关闭时间轮定时器
        timer.stop();
        
        logger.info("WebSocket连接管理器已关闭");
    }

    /**
     * 连接统计信息
     */
    public static class ConnectionStats {
        private final long totalConnections;
        private final long activeConnections;
        private final long heartbeatSentCount;
        private final long heartbeatTimeoutCount;

        public ConnectionStats(long totalConnections, long activeConnections, 
                             long heartbeatSentCount, long heartbeatTimeoutCount) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.heartbeatSentCount = heartbeatSentCount;
            this.heartbeatTimeoutCount = heartbeatTimeoutCount;
        }

        // Getters
        public long getTotalConnections() { return totalConnections; }
        public long getActiveConnections() { return activeConnections; }
        public long getHeartbeatSentCount() { return heartbeatSentCount; }
        public long getHeartbeatTimeoutCount() { return heartbeatTimeoutCount; }
    }
}

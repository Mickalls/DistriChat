package com.distri.chat.infrastructure.websocket;

import com.distri.chat.infrastructure.config.WebSocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * WebSocket服务器启动器
 * 在Spring Boot启动完成后自动启动WebSocket服务器
 */
@Component
public class WebSocketServerRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerRunner.class);
    
    private final NettyWebSocketServer webSocketServer;
    private final WebSocketConfig webSocketConfig;
    
    public WebSocketServerRunner(NettyWebSocketServer webSocketServer, WebSocketConfig webSocketConfig) {
        this.webSocketServer = webSocketServer;
        this.webSocketConfig = webSocketConfig;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 在新线程中启动WebSocket服务器，避免阻塞主线程
        new Thread(() -> {
            try {
                webSocketServer.start(webSocketConfig.getWebsocketPort());
            } catch (Exception e) {
                logger.error("启动WebSocket服务器失败", e);
            }
        }, "WebSocket-Server-Thread").start();
        
        logger.info("WebSocket服务器启动器已执行");
    }

    @PreDestroy
    public void destroy() {
        logger.info("正在关闭WebSocket服务器...");
        webSocketServer.shutdown();
    }
}

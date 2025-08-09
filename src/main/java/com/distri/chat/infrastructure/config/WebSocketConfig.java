package com.distri.chat.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket配置类
 */
@Configuration
public class WebSocketConfig {
    
    @Value("${websocket.port:9000}")
    private int websocketPort;
    
    public int getWebsocketPort() {
        return websocketPort;
    }
}

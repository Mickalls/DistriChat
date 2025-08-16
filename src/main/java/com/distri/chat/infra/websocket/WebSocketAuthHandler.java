package com.distri.chat.infra.websocket;

import com.distri.chat.shared.utils.JwtUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * WebSocket认证处理器
 * 在WebSocket握手阶段进行JWT认证
 */
@Component
public class WebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    // 存储用户信息的Channel属性键
    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> DEVICE_ID_KEY = AttributeKey.valueOf("deviceId");
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthHandler.class);
    private final JwtUtil jwtUtil;
    private final WebSocketConnectionManager connectionManager;

    public WebSocketAuthHandler(JwtUtil jwtUtil, WebSocketConnectionManager connectionManager) {
        this.jwtUtil = jwtUtil;
        this.connectionManager = connectionManager;
    }

    /**
     * 从Channel中获取用户ID
     */
    public static Long getUserId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(USER_ID_KEY).get();
    }

    /**
     * 从Channel中获取设备ID
     */
    public static String getDeviceId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(DEVICE_ID_KEY).get();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            // 只处理WebSocket升级请求
            if (isWebSocketUpgrade(request)) {
                if (!authenticateWebSocket(ctx, request)) {
                    // 认证失败，关闭连接
                    logger.error("[序位3] WebSocketAuthHandler.channelRead - 认证失败，关闭连接");
                    ctx.close();
                    return;
                }
            }
        }

        // 继续传递消息
        super.channelRead(ctx, msg);
    }

    /**
     * 检查是否为WebSocket升级请求
     */
    private boolean isWebSocketUpgrade(FullHttpRequest request) {
        String upgrade = request.headers().get(HttpHeaderNames.UPGRADE);
        return "websocket".equalsIgnoreCase(upgrade);
    }

    /**
     * 认证WebSocket连接
     *
     * @param ctx     Channel上下文
     * @param request HTTP请求
     * @return true-认证成功, false-认证失败
     */
    private boolean authenticateWebSocket(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String token = extractToken(request);
            logger.info("WebSocketAuthHandler.authenticateWebSocket - 提取token: {}", token != null ? "有token" : "无token");

            if (!StringUtils.hasText(token)) {
                logger.error("WebSocketAuthHandler.authenticateWebSocket - WebSocket连接缺少token: {}", ctx.channel().remoteAddress());
                return false;
            }

            // 解析JWT token
            JwtUtil.JwtClaims claims = jwtUtil.parseToken(token);

            // 将用户信息存储到Channel属性中
            ctx.channel().attr(USER_ID_KEY).set(claims.getUserId());
            ctx.channel().attr(DEVICE_ID_KEY).set(claims.getDeviceId());

            String channelId = String.format("user_%d_device_%s", claims.getUserId(), claims.getDeviceId());
            connectionManager.addConnection(channelId, ctx);

            return true;

        } catch (Exception e) {
            logger.error("WebSocketAuthHandler.authenticateWebSocket - WebSocket认证失败: {}", ctx.channel().remoteAddress(), e);
            return false;
        }
    }

    /**
     * 从请求中提取JWT token
     * 支持多种方式：
     * 1. Authorization header: Bearer token
     * 2. Query parameter: ?token=xxx
     */
    private String extractToken(FullHttpRequest request) {
        // 1. 优先从Authorization header获取
        String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 2. 从query parameter获取
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        List<String> tokenParams = decoder.parameters().get("token");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            String token = tokenParams.get(0);
            if (StringUtils.hasText(token)) {
                return token.trim();
            }
        }

        return null;
    }
}

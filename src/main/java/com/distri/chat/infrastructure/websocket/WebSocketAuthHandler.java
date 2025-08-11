package com.distri.chat.infrastructure.websocket;

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

    public WebSocketAuthHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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
        logger.info("[序位3] WebSocketAuthHandler.channelRead - 开始处理HTTP请求: {}", ctx.channel().id().asShortText());

        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            logger.info("[序位3] WebSocketAuthHandler.channelRead - 收到FullHttpRequest: {}", request.uri());

            // 只处理WebSocket升级请求
            if (isWebSocketUpgrade(request)) {
                logger.info("[序位3] WebSocketAuthHandler.channelRead - 检测到WebSocket升级请求");
                if (!authenticateWebSocket(ctx, request)) {
                    // 认证失败，关闭连接
                    logger.error("[序位3] WebSocketAuthHandler.channelRead - 认证失败，关闭连接");
                    ctx.close();
                    return;
                }
                logger.info("[序位3] WebSocketAuthHandler.channelRead - 认证成功，继续处理");
            }
        }

        // 继续传递消息
        logger.info("[序位3] WebSocketAuthHandler.channelRead - 传递消息到下一个handler");
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
        logger.info("[序位3] WebSocketAuthHandler.authenticateWebSocket - 开始认证");
        try {
            String token = extractToken(request);
            logger.info("[序位3] WebSocketAuthHandler.authenticateWebSocket - 提取token: {}", token != null ? "有token" : "无token");

            if (!StringUtils.hasText(token)) {
                logger.warn("[序位3] WebSocketAuthHandler.authenticateWebSocket - WebSocket连接缺少token: {}", ctx.channel().remoteAddress());
                return false;
            }

            // 解析JWT token
            logger.info("[序位3] WebSocketAuthHandler.authenticateWebSocket - 开始解析JWT token");
            JwtUtil.JwtClaims claims = jwtUtil.parseToken(token);
            logger.info("[序位3] WebSocketAuthHandler.authenticateWebSocket - JWT token解析成功");

            // 将用户信息存储到Channel属性中
            ctx.channel().attr(USER_ID_KEY).set(claims.getUserId());
            ctx.channel().attr(DEVICE_ID_KEY).set(claims.getDeviceId());
            logger.info("[序位3] WebSocketAuthHandler.authenticateWebSocket - 用户信息已存储到Channel属性");

            logger.info("[序位3] WebSocketAuthHandler.authenticateWebSocket - WebSocket认证成功: userId={}, deviceId={}, remote={}",
                    claims.getUserId(), claims.getDeviceId(), ctx.channel().remoteAddress());

            return true;

        } catch (Exception e) {
            logger.warn("[序位3] WebSocketAuthHandler.authenticateWebSocket - WebSocket认证失败: remote={}, error={}",
                    ctx.channel().remoteAddress(), e.getMessage());
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

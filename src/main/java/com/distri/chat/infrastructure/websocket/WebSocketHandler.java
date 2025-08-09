package com.distri.chat.infrastructure.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket消息处理器
 */
@Component
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    
    // 存储连接的客户端Channel
    private static final ConcurrentMap<String, ChannelHandlerContext> CHANNELS = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        CHANNELS.put(channelId, ctx);
        logger.info("WebSocket连接建立：{}", channelId);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        CHANNELS.remove(channelId);
        logger.info("WebSocket连接断开：{}", channelId);
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            String channelId = ctx.channel().id().asShortText();
            logger.info("收到WebSocket消息，连接：{}，内容：{}", channelId, text);
            
            // 简单回显消息（后续会替换为实际的消息处理逻辑）
            ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器收到消息：" + text));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            String channelId = ctx.channel().id().asShortText();
            
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                logger.warn("WebSocket连接读超时，关闭连接：{}", channelId);
                ctx.close();
            } else if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                logger.debug("WebSocket连接写超时，发送心跳：{}", channelId);
                ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"heartbeat\"}"));
            } else if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                logger.warn("WebSocket连接读写超时，关闭连接：{}", channelId);
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        logger.error("WebSocket连接异常，连接：{}，错误：", channelId, cause);
        ctx.close();
    }

    /**
     * 获取当前活跃连接数
     */
    public static int getActiveConnectionCount() {
        return CHANNELS.size();
    }

    /**
     * 向指定连接发送消息
     */
    public static boolean sendMessage(String channelId, String message) {
        ChannelHandlerContext ctx = CHANNELS.get(channelId);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(new TextWebSocketFrame(message));
            return true;
        }
        return false;
    }

    /**
     * 广播消息给所有连接
     */
    public static void broadcast(String message) {
        CHANNELS.values().forEach(ctx -> {
            if (ctx.channel().isActive()) {
                ctx.writeAndFlush(new TextWebSocketFrame(message));
            }
        });
    }
}

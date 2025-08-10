package com.distri.chat.infrastructure.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket消息处理器
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    
    private final WebSocketConnectionManager connectionManager;
    private final WebSocketMessageProcessor messageProcessor;
    
    public WebSocketHandler(WebSocketConnectionManager connectionManager, WebSocketMessageProcessor messageProcessor) {
        this.connectionManager = connectionManager;
        this.messageProcessor = messageProcessor;
    }

    /**
     * 处理WebSocket连接激活
     * 
     * 激活时机：
     * - 客户端成功建立WebSocket连接时
     * - 完成HTTP升级握手后
     * 
     * 处理内容：
     * - 将连接注册到连接管理器
     * - 启动心跳检测定时器（75秒）
     * - 增加连接统计计数
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        connectionManager.addConnection(channelId, ctx);
        super.channelActive(ctx);
    }

    /**
     * 处理WebSocket连接关闭
     * 
     * 激活时机：
     * - 客户端主动关闭连接时
     * - 网络异常导致连接断开时
     * - 服务端主动关闭连接时（如心跳超时）
     * - 进程退出或服务器关闭时
     * 
     * 处理内容：
     * - 从连接管理器移除连接
     * - 取消所有相关的定时器（心跳、pong等待）
     * - 清理连接状态和等待队列
     * - 更新连接统计计数
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        connectionManager.removeConnection(channelId);
        super.channelInactive(ctx);
    }

    /**
     * 处理WebSocket消息
     * 
     * 激活时机：
     * - 客户端发送WebSocket文本消息时
     * - 包括心跳消息(ping/pong)和业务消息
     * - 每次收到消息都会触发
     * 
     * 处理内容：
     * - 解析文本帧内容
     * - 委托给消息处理器进行JSON解析和路由
     * - 自动重置心跳检测定时器
     * - 记录调试日志
     * 
     * 注意：只处理TextWebSocketFrame，忽略二进制帧
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String rawMessage = ((TextWebSocketFrame) frame).text();
            String channelId = ctx.channel().id().asShortText();
            
            logger.debug("收到WebSocket消息，连接：{}，内容：{}", channelId, rawMessage);
            
            // 统一使用消息处理器处理标准JSON格式消息
            messageProcessor.processMessage(channelId, rawMessage);
        }
    }

    /**
     * 处理用户事件，如心跳检测、连接超时等
     * 
     * 激活时机：
     * - IdleStateHandler检测到读超时时（75秒无消息）
     * - IdleStateHandler检测到写超时时（我们禁用了）
     * - 其他Netty内部事件触发时
     * 
     * 处理内容：
     * - READER_IDLE：触发服务端主动心跳检测
     * - 委托给连接管理器处理具体的ping/pong逻辑
     * - 记录心跳检测事件
     * 
     * 工作流程：
     * 1. 75秒内没收到客户端消息
     * 2. IdleStateHandler触发READER_IDLE事件
     * 3. 此方法被调用，委托给connectionManager处理
     * 4. connectionManager发送ping并等待pong响应
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            String channelId = ctx.channel().id().asShortText();
            
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                // 75秒没收到客户端消息，开始ping检测
                connectionManager.handleReaderIdle(channelId);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 处理异常情况
     * 
     * 激活时机：
     * - WebSocket连接过程中发生任何异常时
     * - JSON解析错误、网络IO异常等
     * - 消息处理过程中的未捕获异常
     * - Netty pipeline中的异常向上传播
     * 
     * 处理内容：
     * - 记录详细的错误日志
     * - 主动关闭异常连接
     * - 触发channelInactive进行资源清理
     * 
     * 注意：关闭连接会自动触发连接管理器的清理逻辑
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        logger.error("WebSocket连接异常，连接：{}，错误：", channelId, cause);
        ctx.close();
    }
}

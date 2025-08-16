package com.distri.chat.infra.websocket;

import com.distri.chat.shared.utils.JwtUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket服务器
 */
@Component
public class NettyWebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketServer.class);
    private final WebSocketConnectionManager connectionManager;
    private final WebSocketMessageProcessor messageProcessor;
    private final JwtUtil jwtUtil;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public NettyWebSocketServer(WebSocketConnectionManager connectionManager,
                                WebSocketMessageProcessor messageProcessor,
                                JwtUtil jwtUtil) {
        this.connectionManager = connectionManager;
        this.messageProcessor = messageProcessor;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 启动WebSocket服务器
     */
    public void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // HTTP编解码器
                                    .addLast(new HttpServerCodec())
                                    // HTTP消息聚合器
                                    .addLast(new HttpObjectAggregator(65536))
                                    // JWT认证处理器（在WebSocket升级前进行认证）- 每次创建新实例 (非 Sharad Handler)
                                    .addLast(new WebSocketAuthHandler(jwtUtil, connectionManager))
                                    // 支持大文件传输
                                    .addLast(new ChunkedWriteHandler())
                                    // 心跳检测 - 读超时75秒，写超时不检测，读写超时不检测
                                    .addLast(new IdleStateHandler(75, 0, 0, TimeUnit.SECONDS))
                                    // WebSocket协议处理器
                                    .addLast(new WebSocketServerProtocolHandler("/ws"))
                                    // 自定义WebSocket处理器 - 每次创建新实例
                                    .addLast(new WebSocketHandler(connectionManager, messageProcessor));
                        }
                    });

            channelFuture = bootstrap.bind(port).sync();
            logger.info("Netty WebSocket服务器启动成功，端口：{}", port);

        } catch (Exception e) {
            logger.error("Netty WebSocket服务器启动失败", e);
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        try {
            if (channelFuture != null) {
                channelFuture.channel().closeFuture().sync();
            }
        } catch (InterruptedException e) {
            logger.error("关闭WebSocket服务器时发生错误", e);
            Thread.currentThread().interrupt();
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            logger.info("Netty WebSocket服务器已关闭");
        }
    }
}

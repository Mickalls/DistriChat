package com.distri.chat.infrastructure.websocket;

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
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private final WebSocketHandler webSocketHandler;
    
    public NettyWebSocketServer(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
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
                                    // 支持大文件传输
                                    .addLast(new ChunkedWriteHandler())
                                    // 心跳检测 - 读超时60秒，写超时30秒，读写超时90秒
                                    .addLast(new IdleStateHandler(60, 30, 90, TimeUnit.SECONDS))
                                    // WebSocket协议处理器
                                    .addLast(new WebSocketServerProtocolHandler("/ws"))
                                    // 自定义WebSocket处理器
                                    .addLast(webSocketHandler);
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

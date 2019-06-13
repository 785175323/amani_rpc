package com.kakuiwong.server;

import com.kakuiwong.server.annotation.AmaniService;
import com.kakuiwong.server.handler.AmaniServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author gaoyang
 * @email 785175323@qq.com
 */
@Component
public class AmaniServer implements ApplicationContextAware, InitializingBean {

    private final static Logger log = LoggerFactory.getLogger(AmaniServer.class);

    private ApplicationContext applicationContext;
    private Map<String, Object> beans;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        beans = this.applicationContext.getBeansWithAnnotation(AmaniService.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel channel) throws Exception {
                                ChannelPipeline pipeline = channel.pipeline();
                                pipeline.addLast(new ObjectDecoder(1024, ClassResolvers.cacheDisabled(this
                                        .getClass().getClassLoader())));
                                pipeline.addLast(new ObjectEncoder());
                                pipeline.addLast(new AmaniServerHandler(beans));
                            }
                        });
                bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
                bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

                ChannelFuture future = bootstrap.bind("127.0.0.1", 8888).sync();

                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException("Server shutdown!", e);
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }).start();
    }
}

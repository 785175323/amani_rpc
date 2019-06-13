package com.kakuiwong.client.handler;

import com.kakuiwong.common.bean.AmaniResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gaoyang
 * @email 785175323@qq.com
 */
public class ChannelManager {

    private final static Logger log = LoggerFactory.getLogger(ChannelManager.class);

    private volatile static ChannelManager channelManager;

    private ChannelManager() {
    }

    public static ChannelManager getInstance() {
        if (channelManager == null) {
            synchronized (ChannelManager.class) {
                if (channelManager == null) {
                    channelManager = new ChannelManager();
                }
            }
        }
        return channelManager;
    }

    private Map<InetSocketAddress, Channel> channels = new ConcurrentHashMap<>();
    public Map<String, AmaniResponse> responses = new ConcurrentHashMap<>();

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channels.get(inetSocketAddress);
        if (null == channel) {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new RPCChannelInitializer())
                        .option(ChannelOption.SO_KEEPALIVE, true);

                channel = bootstrap.connect(inetSocketAddress.getHostName(), inetSocketAddress.getPort()).sync()
                        .channel();
                registerChannel(inetSocketAddress, channel);
                channel.closeFuture().addListener((ChannelFutureListener) future -> removeChannel(inetSocketAddress));
            } catch (Exception e) {
                log.error("Fail to get channel for address: {}", inetSocketAddress);
            }
        }
        return channel;
    }

    private void registerChannel(InetSocketAddress inetSocketAddress, Channel channel) {
        channels.put(inetSocketAddress, channel);
    }

    private void removeChannel(InetSocketAddress inetSocketAddress) {
        channels.remove(inetSocketAddress);
    }

    private class RPCChannelInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new ObjectDecoder(1024, ClassResolvers.cacheDisabled(this
                    .getClass().getClassLoader())));
            pipeline.addLast(new ObjectEncoder());
            pipeline.addLast(new RPCResponseHandler());
        }
    }

    private class RPCResponseHandler extends SimpleChannelInboundHandler<AmaniResponse> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, AmaniResponse response) throws Exception {
            responses.put(response.getRequestId(), response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("RPC request exception: {}", cause);
        }
    }
}

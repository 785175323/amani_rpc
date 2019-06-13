package com.kakuiwong.server.handler;

import com.kakuiwong.common.bean.AmaniRequest;
import com.kakuiwong.common.bean.AmaniResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author gaoyang
 * @email 785175323@qq.com
 */
public class AmaniServerHandler extends SimpleChannelInboundHandler<AmaniRequest> {
    private final static Logger log = LoggerFactory.getLogger(AmaniServerHandler.class);

    private Map<String, Object> beans;

    public AmaniServerHandler(Map<String, Object> beans) {
        this.beans = beans;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, AmaniRequest request) throws Exception {
        AmaniResponse amaniResponse = new AmaniResponse();
        amaniResponse.setRequestId(request.getRequestId());
        try {
            amaniResponse.setResult(handleRequest(request));
        } catch (Exception e) {
            amaniResponse.setT(e);
            log.error("hanlding request:{}", e);
        }
        channelHandlerContext.writeAndFlush(amaniResponse).addListener((ChannelFutureListener) channelFuture -> {
            log.debug("Sent response for requestId: {},service:{}", request.getRequestId(), request.getInterfaceName());
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("AmaniServer exceptionCaught", cause);
        ctx.close();
    }

    private Object handleRequest(AmaniRequest request) throws Exception {
        Object o = beans.get(request.getInterfaceName());
        if (null == o) {
            throw new RuntimeException("No service bean available:" + request.getInterfaceName());
        }
        Method method = o.getClass().getMethod(request.getMethodName(), request.getParameterTypes());
        method.setAccessible(true);
        return method.invoke(o, request.getParameters());
    }
}

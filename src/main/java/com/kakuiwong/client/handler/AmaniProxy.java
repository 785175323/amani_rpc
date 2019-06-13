package com.kakuiwong.client.handler;

import com.kakuiwong.common.bean.AmaniRequest;
import com.kakuiwong.common.bean.AmaniResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author gaoyang
 * @email 785175323@qq.com
 */
public class AmaniProxy implements FactoryBean<Object> {

    private Class<?> type;

    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public Object getObject() throws Exception {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this::doInvoke);
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        String targetServiceName = type.getName();
        AmaniRequest request = new AmaniRequest();
        request.setRequestId(generateRequestId(targetServiceName));
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameters(args);
        request.setParameterTypes(method.getParameterTypes());

        InetSocketAddress serviceAddress = new InetSocketAddress("127.0.0.1", 8888);

        Channel channel = ChannelManager.getInstance().getChannel(serviceAddress);
        if (null == channel) {
            throw new RuntimeException("Cann't get channel for address" + serviceAddress);
        }
        AmaniResponse response = sendRequest(channel, request);
        if (response == null) {
            throw new RuntimeException("response is null");
        }
        if (response.hasException()) {
            throw response.getT();
        } else {
            return response.getResult();
        }
    }

    private String generateRequestId(String targetServiceName) {
        return targetServiceName + "-" + UUID.randomUUID().toString();
    }

    private AmaniResponse sendRequest(Channel channel, AmaniRequest request) {
        CountDownLatch latch = new CountDownLatch(1);
        channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
        }
        try {
            return ChannelManager.getInstance().responses.get(request.getRequestId());
        } catch (Exception e) {
            return null;
        }
    }
}

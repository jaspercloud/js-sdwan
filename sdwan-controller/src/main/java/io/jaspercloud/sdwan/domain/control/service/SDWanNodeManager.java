package io.jaspercloud.sdwan.domain.control.service;

import io.jaspercloud.sdwan.config.SDWanControllerProperties;
import io.jaspercloud.sdwan.domain.control.entity.Node;
import io.jaspercloud.sdwan.infra.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SDWanNodeManager implements InitializingBean {

    private SDWanControllerProperties properties;
    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public Channel getChannel(String vip) {
        return channelMap.get(vip);
    }

    public List<Channel> getChannelList() {
        return Collections.unmodifiableList(new ArrayList<>(channelMap.values()));
    }

    public Map<String, Node> getNodeMap() {
        Map<String, Node> nodeMap = new HashMap<>();
        for (Map.Entry<String, Channel> entry : channelMap.entrySet()) {
            String key = entry.getKey();
            Node node = AttributeKeys.node(entry.getValue()).get();
            nodeMap.put(key, node);
        }
        return nodeMap;
    }

    public SDWanNodeManager(SDWanControllerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            while (true) {
                Iterator<Map.Entry<String, Channel>> iterator = channelMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    try {
                        Map.Entry<String, Channel> next = iterator.next();
                        Channel node = next.getValue();
                        long diffTime = System.currentTimeMillis() - AttributeKeys.heart(node).get();
                        if (diffTime > properties.getTimeout()) {
                            node.close();
                            iterator.remove();
                        }
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "node-heart-check").start();
    }

    public void addChannel(String vip, Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channelMap.remove(vip);
            }
        });
        AttributeKeys.heart(channel).set(System.currentTimeMillis());
        channelMap.put(vip, channel);
    }

    public void deleteChannel(String vip) {
        Channel channel = channelMap.get(vip);
        if (null == channel) {
            return;
        }
        channel.close();
    }

    public void updateHeart(Channel channel) {
        AttributeKeys.heart(channel).set(System.currentTimeMillis());
    }
}

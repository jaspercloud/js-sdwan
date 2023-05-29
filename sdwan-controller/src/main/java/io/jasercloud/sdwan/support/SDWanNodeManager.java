package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.Attribute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SDWanNodeManager {

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public Map<String, Channel> getChannelMap() {
        return channelMap;
    }

    public void add(String nodeId, Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channelMap.remove(nodeId);
            }
        });
        Attribute<String> nodeIdAttr = AttributeKeys.nodeId(channel);
        nodeIdAttr.set(nodeId);
        channelMap.put(nodeId, channel);
    }
}

package io.jaspercloud.sdwan.app;

import io.jaspercloud.sdwan.domian.Node;
import io.jaspercloud.sdwan.infra.support.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeManager {

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

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

    public void addChannel(String vip, Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channelMap.remove(vip);
            }
        });
        channelMap.put(vip, channel);
    }

    public void deleteChannel(String vip) {
        Channel channel = channelMap.get(vip);
        if (null == channel) {
            return;
        }
        channel.close();
    }
}

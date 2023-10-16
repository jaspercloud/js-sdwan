package io.jaspercloud.sdwan.app;

import io.jaspercloud.sdwan.domian.Node;
import io.jaspercloud.sdwan.infra.support.AttributeKeys;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NodeManager {

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public List<Channel> getChannelList() {
        return Collections.unmodifiableList(new ArrayList<>(channelMap.values()));
    }

    public List<Node> getNodeList() {
        List<Node> nodeList = channelMap.values().stream()
                .map(e -> AttributeKeys.node(e).get())
                .collect(Collectors.toList());
        return Collections.unmodifiableList(nodeList);
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

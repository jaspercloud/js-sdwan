package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.domian.Node;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NodeManager {

    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public List<Node> getNodeList() {
        List<Node> nodeList = channelMap.values().stream()
                .map(e -> AttributeKeys.node(e).get())
                .collect(Collectors.toList());
        return Collections.unmodifiableList(nodeList);
    }

    public void addChannel(Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channelMap.remove(channel.id().asLongText());
            }
        });
        channelMap.put(channel.id().asLongText(), channel);
    }
}

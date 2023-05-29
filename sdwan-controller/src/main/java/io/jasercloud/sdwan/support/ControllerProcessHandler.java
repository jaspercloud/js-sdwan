package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.AttributeKeys;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.Map;

public class ControllerProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.SDWanMessage> {

    private SDWanControllerProperties properties;
    private SDWanNodeManager nodeManager;

    public ControllerProcessHandler(SDWanControllerProperties properties,
                                    SDWanNodeManager nodeManager) {
        this.properties = properties;
        this.nodeManager = nodeManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.SDWanMessage msg) throws Exception {
        switch (msg.getType().getNumber()) {
            case SDWanProtos.MsgType.AuthReq_VALUE: {
                SDWanProtos.SDWanAuthReq authReq = SDWanProtos.SDWanAuthReq.parseFrom(msg.getData());
                boolean authResult = auth(ctx.channel(), msg, authReq);
                if (authResult) {
                    updateNodeList();
                }
                break;
            }
        }
    }

    private void updateNodeList() {
        SDWanProtos.SDWanNodeList.Builder builder = SDWanProtos.SDWanNodeList.newBuilder();
        Map<String, Channel> channelMap = nodeManager.getChannelMap();
        for (Map.Entry<String, Channel> entry : channelMap.entrySet()) {
            String itemNodeId = entry.getKey();
            Channel channel = entry.getValue();
            String nodeIP = AttributeKeys.nodeIP(channel).get();
            String itemVip = AttributeKeys.vip(channel).get();
            Integer nodeUdpPort = AttributeKeys.nodeUdpPort(channel).get();
            builder.addNode(SDWanProtos.SDWanNode.newBuilder()
                    .setNodeId(itemNodeId)
                    .setNodeIP(nodeIP)
                    .setVip(itemVip)
                    .setNodeUdpPort(nodeUdpPort)
                    .build());
        }
        SDWanProtos.SDWanNodeList nodeList = builder.build();
        for (Map.Entry<String, Channel> entry : channelMap.entrySet()) {
            Channel ch = entry.getValue();
            String channelId = AttributeKeys.channelId(ch).get();
            SDWanProtos.SDWanMessage message = SDWanProtos.SDWanMessage
                    .newBuilder()
                    .setChannelId(channelId)
                    .setType(SDWanProtos.MsgType.NodeList)
                    .setData(nodeList.toByteString())
                    .build();
            ch.writeAndFlush(message);
        }
    }

    private boolean auth(Channel channel, SDWanProtos.SDWanMessage sdWanMessage, SDWanProtos.SDWanAuthReq authReq) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        String vip = properties.getNodeIpMap().get(authReq.getNodeId());
        if (StringUtils.isEmpty(vip)) {
            SDWanProtos.SDWanMessage message = SDWanProtos.SDWanMessage
                    .newBuilder()
                    .setChannelId(sdWanMessage.getChannelId())
                    .setType(SDWanProtos.MsgType.AuthRespFail)
                    .build();
            channel.writeAndFlush(message);
            return false;
        }
        AttributeKeys.channelId(channel).set(sdWanMessage.getChannelId());
        AttributeKeys.nodeId(channel).set(authReq.getNodeId());
        AttributeKeys.nodeIP(channel).set(address.getHostName());
        AttributeKeys.vip(channel).set(vip);
        AttributeKeys.nodeUdpPort(channel).set(authReq.getNodeUdpPort());
        nodeManager.add(authReq.getNodeId(), channel);
        SDWanProtos.SDWanAuthResp authResp = SDWanProtos.SDWanAuthResp
                .newBuilder()
                .setNodeIP(address.getHostName())
                .setVip(vip)
                .setNetmaskPrefix(properties.getNetmaskPrefix())
                .build();
        SDWanProtos.SDWanMessage message = SDWanProtos.SDWanMessage
                .newBuilder()
                .setChannelId(sdWanMessage.getChannelId())
                .setType(SDWanProtos.MsgType.AuthRespSuccess)
                .setData(authResp.toByteString())
                .build();
        channel.writeAndFlush(message);
        return true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}

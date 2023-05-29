package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.AttributeKeys;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

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
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        switch (msg.getType().getNumber()) {
            case SDWanProtos.MsgType.AuthReq_VALUE: {
                SDWanProtos.SDWanAuthReq authReq = SDWanProtos.SDWanAuthReq.parseFrom(msg.getData());
                String nodeName = authReq.getNodeName();
                AttributeKeys.nodeIP(ctx.channel()).set(address.getHostName());
                String vip = properties.getNodeIpMap().get(nodeName);
                AttributeKeys.vip(ctx.channel()).set(vip);
                AttributeKeys.nodeUdpPort(ctx.channel()).set(authReq.getNodeUdpPort());
                nodeManager.add(nodeName, ctx.channel());
                {
                    SDWanProtos.SDWanAuthResp authResp = SDWanProtos.SDWanAuthResp
                            .newBuilder()
                            .setNodeIP(address.getHostName())
                            .setVip(vip)
                            .setNetmaskPrefix(properties.getNetmaskPrefix())
                            .build();
                    SDWanProtos.SDWanMessage message = SDWanProtos.SDWanMessage
                            .newBuilder()
                            .setChannelId(msg.getChannelId())
                            .setType(SDWanProtos.MsgType.AuthResp)
                            .setData(authResp.toByteString())
                            .build();
                    ctx.channel().writeAndFlush(message);
                }
                {
                    SDWanProtos.SDWanNodeList.Builder builder = SDWanProtos.SDWanNodeList.newBuilder();
                    Map<String, Channel> channelMap = nodeManager.getChannelMap();
                    for (Map.Entry<String, Channel> entry : channelMap.entrySet()) {
                        String itemNodeName = entry.getKey();
                        Channel channel = entry.getValue();
                        String nodeIP = AttributeKeys.nodeIP(channel).get();
                        String itemVip = AttributeKeys.vip(channel).get();
                        Integer nodeUdpPort = AttributeKeys.nodeUdpPort(channel).get();
                        builder.addNode(SDWanProtos.SDWanNode.newBuilder()
                                .setNodeName(itemNodeName)
                                .setNodeIP(nodeIP)
                                .setVip(itemVip)
                                .setNodeUdpPort(nodeUdpPort)
                                .build());
                    }
                    SDWanProtos.SDWanNodeList nodeList = builder.build();
                    SDWanProtos.SDWanMessage message = SDWanProtos.SDWanMessage
                            .newBuilder()
                            .setChannelId(msg.getChannelId())
                            .setType(SDWanProtos.MsgType.NodeList)
                            .setData(nodeList.toByteString())
                            .build();
                    for (Map.Entry<String, Channel> entry : channelMap.entrySet()) {
                        Channel ch = entry.getValue();
                        ch.writeAndFlush(message);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}

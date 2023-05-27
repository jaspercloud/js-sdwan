package io.jasercloud.sdwan.support;

import demo.WinTun;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.SDWanMessage> {

    private SDWanNodeProperties properties;
    private MemberNodeManager nodeManager;

    public NodeProcessHandler(SDWanNodeProperties properties,
                              MemberNodeManager nodeManager) {
        this.properties = properties;
        this.nodeManager = nodeManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SDWanProtos.SDWanAuthReq authReq = SDWanProtos.SDWanAuthReq.newBuilder()
                .setNodeName(properties.getNodeName())
                .build();
        SDWanProtos.SDWanMessage message = SDWanProtos.SDWanMessage.newBuilder()
                .setChannelId(ctx.channel().id().asShortText())
                .setType(SDWanProtos.MsgType.AuthReq)
                .setData(authReq.toByteString())
                .build();
        ctx.channel().writeAndFlush(message);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.SDWanMessage msg) throws Exception {
        switch (msg.getType().getNumber()) {
            case SDWanProtos.MsgType.AuthResp_VALUE: {
                SDWanProtos.SDWanAuthResp authResp = SDWanProtos.SDWanAuthResp.parseFrom(msg.getData());
                log.info("authResp: nodeIP={}, vip={}", authResp.getNodeIP(), authResp.getVip());
                WinTun winTun = new WinTun(authResp.getVip(), authResp.getNetmaskPrefix());
                winTun.start(properties.getNodeName());
                break;
            }
            case SDWanProtos.MsgType.NodeList_VALUE: {
                SDWanProtos.SDWanNodeList nodeList = SDWanProtos.SDWanNodeList.parseFrom(msg.getData());
                for (SDWanProtos.SDWanNode node : nodeList.getNodeList()) {
                    log.info("nodeList: nodeName={}, vip={}", node.getNodeName(), node.getVip());
                    nodeManager.add(node.getNodeName(), node.getVip());
                }
                break;
            }
        }
    }
}

package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.WinTun;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NodeProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.SDWanMessage> {

    private SDWanNodeProperties properties;
    private SDWanNodeInfoManager nodeManager;
    private WinTun winTun;
    private UdpNode udpNode;

    public NodeProcessHandler(SDWanNodeProperties properties,
                              SDWanNodeInfoManager nodeManager,
                              WinTun winTun,
                              UdpNode udpNode) {
        this.properties = properties;
        this.nodeManager = nodeManager;
        this.winTun = winTun;
        this.udpNode = udpNode;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SDWanProtos.SDWanAuthReq authReq = SDWanProtos.SDWanAuthReq.newBuilder()
                .setNodeId(properties.getNodeId())
                .setNodeUdpPort(properties.getNodeUdpPort())
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
            case SDWanProtos.MsgType.AuthRespSuccess_VALUE: {
                SDWanProtos.SDWanAuthResp authResp = SDWanProtos.SDWanAuthResp.parseFrom(msg.getData());
                log.info("authResp: nodeIP={}, vip={}", authResp.getNodeIP(), authResp.getVip());
                winTun.start(authResp.getVip(), authResp.getNetmaskPrefix());
                break;
            }
            case SDWanProtos.MsgType.AuthRespFail_VALUE: {
                log.info("authResp failed");
                break;
            }
            case SDWanProtos.MsgType.NodeList_VALUE: {
                SDWanProtos.SDWanNodeList nodeList = SDWanProtos.SDWanNodeList.parseFrom(msg.getData());
                List<SDWanNodeInfo> nodeInfoList = new ArrayList<>();
                for (SDWanProtos.SDWanNode node : nodeList.getNodeList()) {
                    log.info("nodeList: nodeId={}, nodeIP={}, vip={}, nodeUdpPort={}",
                            node.getNodeId(), node.getNodeIP(), node.getVip(), node.getNodeUdpPort());
                    SDWanNodeInfo nodeInfo = new SDWanNodeInfo();
                    nodeInfo.setNodeId(node.getNodeId());
                    nodeInfo.setNodeIP(node.getNodeIP());
                    nodeInfo.setVip(node.getVip());
                    nodeInfo.setNodeUdpPort(node.getNodeUdpPort());
                    nodeInfoList.add(nodeInfo);
                }
                nodeManager.updateList(nodeInfoList);
                break;
            }
        }
    }
}

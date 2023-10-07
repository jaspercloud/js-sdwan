package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.netty.channel.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@ChannelHandler.Sharable
public class SDWanControllerProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> implements InitializingBean {

    private SDWanControllerProperties properties;

    //key: ip, value: channel
    private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();

    private Cidr cidr;

    public SDWanControllerProcessHandler(SDWanControllerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cidr = Cidr.parseCidr(properties.getCidr());
        cidr.getIpList().forEach(item -> {
            bindIPMap.put(item, new AtomicReference<>());
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
        Channel channel = ctx.channel();
        switch (request.getType().getNumber()) {
            case SDWanProtos.MsgTypeCode.HeartType_VALUE: {
                processHeart(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.RegReqType_VALUE: {
                processReg(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.NodeArpReqType_VALUE: {
                processSDArp(channel, request);
                break;
            }
        }
    }

    private Channel findNodeByIP(String ip) {
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
            Channel targetChannel = entry.getValue().get();
            if (null == targetChannel) {
                continue;
            }
            NodeInfo nodeInfo = AttributeKeys.nodeInfo(targetChannel).get();
            String vip = nodeInfo.getVip();
            if (StringUtils.equals(vip, ip)) {
                return targetChannel;
            }
            if (NodeType.Mesh.equals(nodeInfo.getNodeType())) {
                Cidr cidr = nodeInfo.getMeshCidr();
                if (null == cidr) {
                    continue;
                }
                if (cidr.getIpList().contains(ip)) {
                    return targetChannel;
                }
            }
        }
        return null;
    }

    private void processSDArp(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.parseFrom(request.getData());
        Channel targetChannel = findNodeByIP(nodeArpReq.getIp());
        if (null == targetChannel) {
            SDWanProtos.SDArpResp arpResp = SDWanProtos.SDArpResp.newBuilder()
                    .setCode(1)
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.NodeArpRespType)
                    .setData(arpResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            return;
        }
        NodeInfo nodeInfo = AttributeKeys.nodeInfo(targetChannel).get();
        String vip = nodeInfo.getVip();
        String host = nodeInfo.getPublicAddress().getHostString();
        int port = nodeInfo.getPublicAddress().getPort();
        SDWanProtos.SDArpResp arpResp = SDWanProtos.SDArpResp.newBuilder()
                .setCode(0)
                .setPublicIP(host)
                .setPublicPort(port)
                .setVip(vip)
                .build();
        SDWanProtos.Message response = request.toBuilder()
                .setType(SDWanProtos.MsgTypeCode.NodeArpRespType)
                .setData(arpResp.toByteString())
                .build();
        channel.writeAndFlush(response);
    }

    private void processHeart(Channel channel, SDWanProtos.Message request) {
        channel.writeAndFlush(request);
    }

    private void processReg(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.RegReq regReq = SDWanProtos.RegReq.parseFrom(request.getData());
        try {
            String macAddress = regReq.getMacAddress();
            NodeInfo nodeInfo = AttributeKeys.nodeInfo(channel).get();
            if (null == nodeInfo) {
                nodeInfo = new NodeInfo();
                String vip = bindStaticNode(macAddress, channel);
                if (null == vip) {
                    if (SDWanProtos.NodeTypeCode.MeshType.equals(regReq.getNodeType())) {
                        throw new ProcessCodeException(SDWanProtos.MessageCode.NodeTypeError_VALUE);
                    }
                    vip = bindDynamicNode(channel);
                }
                if (null == vip) {
                    throw new ProcessCodeException(SDWanProtos.MessageCode.NotEnough_VALUE);
                }
                nodeInfo.setNodeType(NodeType.valueOf(regReq.getNodeType().getNumber()));
                nodeInfo.setMacAddress(macAddress);
                nodeInfo.setVip(vip);
                nodeInfo.setPublicAddress(new InetSocketAddress(regReq.getPublicIP(), regReq.getPublicPort()));
                if (SDWanProtos.NodeTypeCode.MeshType.equals(regReq.getNodeType())) {
                    nodeInfo.setMeshCidr(Cidr.parseCidr(regReq.getMeshCidr()));
                }
                AttributeKeys.nodeInfo(channel).set(nodeInfo);
            }
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(0)
                    .setVip(nodeInfo.getVip())
                    .setMaskBits(cidr.getMaskBits())
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.RegRespType)
                    .setData(regResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
        } catch (ProcessCodeException e) {
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(e.getCode())
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.RegRespType)
                    .setData(regResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
        }
    }

    private String bindStaticNode(String macAddress, Channel channel) {
        for (Map.Entry<String, SDWanControllerProperties.Node> entry : properties.getStaticNodes().entrySet()) {
            SDWanControllerProperties.Node node = entry.getValue();
            if (StringUtils.equals(node.getMacAddress(), macAddress)) {
                AtomicReference<Channel> ref = bindIPMap.get(node.getVip());
                if (ref.compareAndSet(null, channel)) {
                    channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            ref.set(null);
                        }
                    });
                    return node.getVip();
                }
            }
        }
        return null;
    }

    private String bindDynamicNode(Channel channel) {
        List<String> staticIPList = properties.getStaticNodes().values().stream().map(e -> e.getVip())
                .collect(Collectors.toList());
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
            if (staticIPList.contains(entry.getKey())) {
                //过滤静态IP
                continue;
            }
            AtomicReference<Channel> ref = entry.getValue();
            if (ref.compareAndSet(null, channel)) {
                channel.closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        ref.set(null);
                    }
                });
                String vip = entry.getKey();
                return vip;
            }
        }
        return null;
    }
}

package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.AttributeKeys;
import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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
            case SDWanProtos.MsgType.HeartType_VALUE: {
                processHeart(channel, request);
                break;
            }
            case SDWanProtos.MsgType.RegReqType_VALUE: {
                processReg(channel, request);
                break;
            }
            case SDWanProtos.MsgType.NodeArpReqType_VALUE: {
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
            String vip = AttributeKeys.nodeVip(targetChannel).get();
            if (StringUtils.equals(vip, ip)) {
                return targetChannel;
            }
            Cidr cidr = AttributeKeys.nodeCidr(targetChannel).get();
            if (null == cidr) {
                continue;
            }
            if (cidr.getIpList().contains(ip)) {
                return targetChannel;
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
                    .setType(SDWanProtos.MsgType.NodeArpRespType)
                    .setData(arpResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            return;
        }
        InetSocketAddress address = AttributeKeys.nodePublicAddress(targetChannel).get();
        String vip = AttributeKeys.nodeVip(targetChannel).get();
        String host = address.getHostString();
        int port = address.getPort();
        SDWanProtos.SDArpResp arpResp = SDWanProtos.SDArpResp.newBuilder()
                .setCode(0)
                .setPublicIP(host)
                .setPublicPort(port)
                .setVip(vip)
                .build();
        SDWanProtos.Message response = request.toBuilder()
                .setType(SDWanProtos.MsgType.NodeArpRespType)
                .setData(arpResp.toByteString())
                .build();
        channel.writeAndFlush(response);
    }

    private void processHeart(Channel channel, SDWanProtos.Message request) {
        channel.writeAndFlush(request);
    }

    private void processReg(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.RegReq regReq = SDWanProtos.RegReq.parseFrom(request.getData());
        String hardwareAddress = regReq.getHardwareAddress();
        String vip = AttributeKeys.nodeVip(channel).get();
        if (null == vip) {
            vip = bindStaticNode(hardwareAddress, channel);
            if (null == vip) {
                if (SDWanProtos.NodeType.MeshType.equals(regReq.getNodeType())) {
                    SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                            .setCode(SDWanProtos.MessageCode.NodeTypeError_VALUE)
                            .build();
                    SDWanProtos.Message response = request.toBuilder()
                            .setType(SDWanProtos.MsgType.RegRespType)
                            .setData(regResp.toByteString())
                            .build();
                    channel.writeAndFlush(response);
                    return;
                }
                vip = bindDynamicNode(channel);
            }
        }
        if (null == vip) {
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.NotFoundVIP_VALUE)
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgType.RegRespType)
                    .setData(regResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            return;
        }
        AttributeKeys.nodeVip(channel)
                .set(vip);
        AttributeKeys.nodePublicAddress(channel)
                .set(new InetSocketAddress(regReq.getPublicIP(), regReq.getPublicPort()));
        AttributeKeys.nodeHardwareAddress(channel)
                .set(regReq.getHardwareAddress());
        if (SDWanProtos.NodeType.MeshType.equals(regReq.getNodeType())) {
            AttributeKeys.nodeCidr(channel)
                    .set(Cidr.parseCidr(regReq.getCidr()));
        }
        SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                .setCode(0)
                .setVip(vip)
                .setMaskBits(cidr.getMaskBits())
                .build();
        SDWanProtos.Message response = request.toBuilder()
                .setType(SDWanProtos.MsgType.RegRespType)
                .setData(regResp.toByteString())
                .build();
        channel.writeAndFlush(response);
    }

    private String bindStaticNode(String hardwareAddress, Channel channel) {
        for (Map.Entry<String, SDWanControllerProperties.Node> entry : properties.getStaticNodes().entrySet()) {
            SDWanControllerProperties.Node node = entry.getValue();
            if (StringUtils.equals(node.getHardwareAddress(), hardwareAddress)) {
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
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
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

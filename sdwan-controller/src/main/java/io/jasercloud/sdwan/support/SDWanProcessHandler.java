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
public class SDWanProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> implements InitializingBean {

    private SDWanControllerProperties properties;

    //key: ip, value: channel
    private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();

    private Cidr cidr;

    public SDWanProcessHandler(SDWanControllerProperties properties) {
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
                processNodeArp(channel, request);
                break;
            }
        }
    }

    private void processNodeArp(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.NodeArpReq nodeArpReq = SDWanProtos.NodeArpReq.parseFrom(request.getData());
        AtomicReference<Channel> reference = bindIPMap.get(nodeArpReq.getVip());
        Channel targetChannel = reference.get();
        if (null == targetChannel) {
            SDWanProtos.NodeArpResp arpResp = SDWanProtos.NodeArpResp.newBuilder()
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
        String host = address.getHostString();
        int port = address.getPort();
        SDWanProtos.NodeArpResp arpResp = SDWanProtos.NodeArpResp.newBuilder()
                .setCode(0)
                .setPublicAddress(host)
                .setPublicPort(port)
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
        String vip = bindStaticNode(hardwareAddress, channel);
        if (null == vip) {
            vip = bindDynamicNode(channel);
        }
        if (null == vip) {
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(1)
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgType.RegRespType)
                    .setData(regResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            return;
        }
        AtomicReference<Channel> reference = bindIPMap.get(vip);
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                reference.set(null);
            }
        });
        AttributeKeys.nodePublicAddress(channel)
                .set(new InetSocketAddress(regReq.getPublicAddress(), regReq.getPublicPort()));
        AttributeKeys.nodeHardwareAddress(channel)
                .set(regReq.getHardwareAddress());
        SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                .setCode(0)
                .setVip(vip)
                .setMask(cidr.getMaskAddress())
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
                String vip = entry.getKey();
                return vip;
            }
        }
        return null;
    }
}

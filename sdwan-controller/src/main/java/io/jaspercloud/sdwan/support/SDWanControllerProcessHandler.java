package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@ChannelHandler.Sharable
public class SDWanControllerProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> implements InitializingBean {

    private SDWanControllerProperties properties;

    //key: ip, value: channel
    private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();
    //key: vip, value: destination
    private MultiValueMap<String, String> routeMap = new LinkedMultiValueMap<>();
    private Cidr ipPool;

    public SDWanControllerProcessHandler(SDWanControllerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ipPool = Cidr.parseCidr(properties.getCidr());
        ipPool.getIpList()
                .forEach(item -> {
                    bindIPMap.put(item, new AtomicReference<>());
                });
        properties.getStaticRoutes().values()
                .forEach(e -> {
                    routeMap.add(e.getNextHop(), e.getDestination());
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
            case SDWanProtos.MsgTypeCode.SDArpReqType_VALUE: {
                processSDArp(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.PunchingType_VALUE: {
                processPunching(channel, request);
                break;
            }
        }
    }

    private void processPunching(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.Punching punching = SDWanProtos.Punching.parseFrom(request.getData());
        String dstVIP = punching.getDstVIP();
        Channel targetChannel = findNodeByIP(dstVIP);
        if (null == targetChannel) {
            return;
        }
        targetChannel.writeAndFlush(request);
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
                List<Cidr> routeList = nodeInfo.getRouteList();
                for (Cidr route : routeList) {
                    if (route.getIpList().contains(ip)) {
                        return targetChannel;
                    }
                }
            }
        }
        return null;
    }

    private void processSDArp(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.parseFrom(request.getData());
        String ip = nodeArpReq.getIp();
        Channel targetChannel = findNodeByIP(ip);
        log.debug("sdArp: ip={}, findNode={}", ip, null != targetChannel);
        if (null == targetChannel) {
            SDWanProtos.SDArpResp arpResp = SDWanProtos.SDArpResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.NotFoundSDArp_VALUE)
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.SDArpRespType)
                    .setData(arpResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            return;
        }
        NodeInfo nodeInfo = AttributeKeys.nodeInfo(targetChannel).get();
        String vip = nodeInfo.getVip();
        log.debug("sdArp: ip={}, vip={}", ip, vip);
        SDWanProtos.SDArpResp.Builder sdArpBuilder = SDWanProtos.SDArpResp.newBuilder()
                .setCode(SDWanProtos.MessageCode.Success_VALUE)
                .setVip(vip)
                .setInternalAddr(SDWanProtos.SocketAddress.newBuilder()
                        .setIp(nodeInfo.getInternalAddress().getHostString())
                        .setPort(nodeInfo.getInternalAddress().getPort())
                        .build())
                .setStunMapping(nodeInfo.getStunMapping())
                .setStunFiltering(nodeInfo.getStunFiltering())
                .setTtl(properties.getSdArpTTL());
        if (null != nodeInfo.getPublicAddress()) {
            sdArpBuilder.setPublicAddr(SDWanProtos.SocketAddress.newBuilder()
                    .setIp(nodeInfo.getPublicAddress().getHostString())
                    .setPort(nodeInfo.getPublicAddress().getPort())
                    .build());
        }
        SDWanProtos.SDArpResp arpResp = sdArpBuilder.build();
        SDWanProtos.Message response = request.toBuilder()
                .setType(SDWanProtos.MsgTypeCode.SDArpRespType)
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
            NodeInfo nodeInfo = AttributeKeys.nodeInfo(channel).get();
            if (null == nodeInfo) {
                String vip = bindVip(regReq, channel);
                nodeInfo = new NodeInfo();
                nodeInfo.setNodeType(NodeType.valueOf(regReq.getNodeType().getNumber()));
                nodeInfo.setMacAddress(regReq.getMacAddress());
                nodeInfo.setVip(vip);
                nodeInfo.setInternalAddress(new InetSocketAddress(regReq.getInternalAddr().getIp(), regReq.getInternalAddr().getPort()));
                nodeInfo.setStunMapping(regReq.getStunMapping());
                nodeInfo.setStunFiltering(regReq.getStunFiltering());
                nodeInfo.setPublicAddress(new InetSocketAddress(regReq.getPublicAddr().getIp(), regReq.getPublicAddr().getPort()));
                if (SDWanProtos.NodeTypeCode.MeshType.equals(regReq.getNodeType())) {
                    List<String> routeList = routeMap.get(vip);
                    for (String route : routeList) {
                        nodeInfo.getRouteList().add(Cidr.parseCidr(route));
                    }
                }
                AttributeKeys.nodeInfo(channel).set(nodeInfo);
                log.info("reg: nodeType={}, macAddr={}, vip={}, publicAddr={}, mapping={}, filtering={}",
                        nodeInfo.getNodeType(),
                        nodeInfo.getMacAddress(),
                        nodeInfo.getVip(),
                        nodeInfo.getPublicAddress(),
                        nodeInfo.getStunMapping(),
                        nodeInfo.getStunFiltering());
            }
            String vip = nodeInfo.getVip();
            List<String> routes = routeMap.entrySet().stream()
                    .filter(e -> !StringUtils.equals(e.getKey(), vip))
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.Success_VALUE)
                    .setVip(vip)
                    .setMaskBits(ipPool.getMaskBits())
                    .addAllRoute(routes)
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

    private String bindVip(SDWanProtos.RegReq regReq, Channel channel) {
        String vip = bindStaticNode(regReq.getMacAddress(), channel);
        if (null == vip) {
            if (SDWanProtos.NodeTypeCode.MeshType.equals(regReq.getNodeType())) {
                throw new ProcessCodeException(SDWanProtos.MessageCode.NodeTypeError_VALUE);
            }
            vip = bindDynamicNode(channel);
        }
        if (null == vip) {
            throw new ProcessCodeException(SDWanProtos.MessageCode.NotEnough_VALUE);
        }
        return vip;
    }

    private String bindStaticNode(String macAddress, Channel channel) {
        for (Map.Entry<String, SDWanControllerProperties.StaticNode> entry : properties.getStaticNodes().entrySet()) {
            SDWanControllerProperties.StaticNode node = entry.getValue();
            if (StringUtils.equals(node.getMacAddress(), macAddress)) {
                AtomicReference<Channel> ref = bindIPMap.get(node.getVip());
                //CAS
                if (!ref.compareAndSet(null, channel)) {
                    throw new ProcessCodeException(SDWanProtos.MessageCode.VipBound_VALUE);
                }
                channel.closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        ref.set(null);
                    }
                });
                return node.getVip();
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
            //CAS
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
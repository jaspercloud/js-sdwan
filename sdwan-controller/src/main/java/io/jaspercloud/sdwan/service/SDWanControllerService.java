package io.jaspercloud.sdwan.service;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.config.SDWanControllerProperties;
import io.jaspercloud.sdwan.controller.param.RouteDTO;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.model.Node;
import io.jaspercloud.sdwan.repository.NodeRepository;
import io.jaspercloud.sdwan.repository.RouteRepository;
import io.jaspercloud.sdwan.support.AttributeKeys;
import io.jaspercloud.sdwan.support.NodeType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SDWanControllerService implements InitializingBean {

    @Resource
    private SDWanControllerProperties properties;

    @Resource
    private NodeRepository nodeRepository;

    @Resource
    private RouteRepository routeRepository;

    @Resource
    private ConfigService configService;

    @Resource
    private NodeManager nodeManager;

    //key: ip, value: channel
    private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();
    private Cidr ipPool;

    @Override
    public void afterPropertiesSet() throws Exception {
        ipPool = Cidr.parseCidr(properties.getCidr());
        ipPool.getAvailableIpList()
                .forEach(item -> {
                    bindIPMap.put(item, new AtomicReference<>());
                });
    }

    public void regist(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.RegReq regReq = SDWanProtos.RegReq.parseFrom(request.getData());
            Node node = AttributeKeys.node(channel).get();
            if (null == node) {
                node = new Node();
                node.setMacAddress(regReq.getMacAddress());
                node.setNodeType(NodeType.valueOf(regReq.getNodeType().getNumber()));
                node.setInternalAddress(new InetSocketAddress(regReq.getInternalAddr().getIp(), regReq.getInternalAddr().getPort()));
                node.setMappingType(regReq.getMappingType().name());
                node.setPublicAddress(new InetSocketAddress(regReq.getPublicAddr().getIp(), regReq.getPublicAddr().getPort()));
                node.setRelayToken(regReq.getRelayToken());
                if (SDWanProtos.NodeTypeCode.MeshType.equals(regReq.getNodeType())) {
                    List<RouteDTO> routeList = configService.getRouteList();
                    for (RouteDTO route : routeList) {
                        if (StringUtils.equals(route.getNexthop(), node.getVip())) {
                            continue;
                        }
                        node.getRouteList().add(Cidr.parseCidr(route.getDestination()));
                    }
                }
                bindVip(regReq, channel, node);
                AttributeKeys.node(channel).set(node);
                log.info("reg: nodeType={}, macAddr={}, vip={}, mappingType={}, publicAddr={}, relayToken={}",
                        node.getNodeType(),
                        node.getMacAddress(),
                        node.getVip(),
                        node.getMappingType(),
                        node.getPublicAddress(),
                        node.getRelayToken());
            }
            String vip = node.getVip();
            nodeManager.addChannel(vip, channel);
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.Success_VALUE)
                    .setVip(vip)
                    .setMaskBits(ipPool.getMaskBits())
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
            channel.close();
        } catch (Exception e) {
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError_VALUE)
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.RegRespType)
                    .setData(regResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            channel.close();
        }
    }

//    public SDWanProtos.SDArpResp sdArp(Channel channel, SDWanProtos.Message request) {
//        try {
//            SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.parseFrom(request.getData());
//            String ip = nodeArpReq.getIp();
//            Channel targetChannel = findNodeByIP(ip);
//            log.debug("sdArp: ip={}, findNode={}", ip, null != targetChannel);
//            if (null == targetChannel) {
//                SDWanProtos.SDArpResp arpResp = SDWanProtos.SDArpResp.newBuilder()
//                        .setCode(SDWanProtos.MessageCode.NotFoundSDArp_VALUE)
//                        .build();
//                return arpResp;
//            }
//            Node node = AttributeKeys.node(targetChannel).get();
//            String vip = node.getVip();
//            log.debug("sdArp: ip={}, vip={}", ip, vip);
//            SDWanProtos.SDArpResp.Builder sdArpBuilder = SDWanProtos.SDArpResp.newBuilder()
//                    .setCode(SDWanProtos.MessageCode.Success_VALUE)
//                    .setVip(vip)
//                    .setInternalAddr(SDWanProtos.SocketAddress.newBuilder()
//                            .setIp(node.getInternalAddress().getHostString())
//                            .setPort(node.getInternalAddress().getPort())
//                            .build())
//                    .setStunMapping(node.getStunMapping())
//                    .setStunFiltering(node.getStunFiltering())
//                    .setTtl(properties.getSdArpTTL());
//            if (null != node.getPublicAddress()) {
//                sdArpBuilder.setPublicAddr(SDWanProtos.SocketAddress.newBuilder()
//                        .setIp(node.getPublicAddress().getHostString())
//                        .setPort(node.getPublicAddress().getPort())
//                        .build());
//            }
//            SDWanProtos.SDArpResp arpResp = sdArpBuilder.build();
//            return arpResp;
//        } catch (Exception e) {
//            SDWanProtos.SDArpResp arpResp = SDWanProtos.SDArpResp.newBuilder()
//                    .setCode(SDWanProtos.MessageCode.SysError_VALUE)
//                    .build();
//            return arpResp;
//        }
//    }

    public Channel findNodeByIP(String ip) {
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
            Channel targetChannel = entry.getValue().get();
            if (null == targetChannel) {
                continue;
            }
            Node node = AttributeKeys.node(targetChannel).get();
            String vip = node.getVip();
            if (StringUtils.equals(vip, ip)) {
                return targetChannel;
            }
            if (NodeType.Mesh.equals(node.getNodeType())) {
                List<Cidr> routeList = node.getRouteList();
                for (Cidr route : routeList) {
                    if (route.getIpList().contains(ip)) {
                        return targetChannel;
                    }
                }
            }
        }
        return null;
    }

    private void bindVip(SDWanProtos.RegReq regReq, Channel channel, Node node) {
        Node queryNode = nodeRepository.queryByMacAddress(regReq.getMacAddress());
        if (null != queryNode) {
            applyChannel(queryNode.getVip(), channel);
            node.setVip(queryNode.getVip());
            return;
        }
        String vip = applyVIP(channel);
        if (null == vip) {
            throw new ProcessCodeException(SDWanProtos.MessageCode.NotEnough_VALUE);
        }
        node.setVip(vip);
        nodeRepository.save(node);
    }

    private void applyChannel(String vip, Channel channel) {
        AtomicReference<Channel> ref = bindIPMap.get(vip);
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
    }

    private String applyVIP(Channel channel) {
        List<Node> nodeList = nodeRepository.queryList();
        List<String> ipList = nodeList.stream().map(e -> e.getVip())
                .collect(Collectors.toList());
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
            if (ipList.contains(entry.getKey())) {
                //过滤已分配IP
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

    public void processRouteList(Channel channel, SDWanProtos.Message request) {
        Map<Long, Node> nodeMap = nodeRepository.getMeshNodeList()
                .stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        List<SDWanProtos.Route> routes = routeRepository.queryList()
                .stream()
                .map(e -> {
                    Node node = nodeMap.get(e.getMeshId());
                    SDWanProtos.Route route = SDWanProtos.Route.newBuilder()
                            .setDestination(e.getDestination())
                            .setNexthop(node.getVip())
                            .build();
                    return route;
                })
                .collect(Collectors.toList());
        SDWanProtos.RouteList routeList = SDWanProtos.RouteList.newBuilder()
                .addAllRoute(routes)
                .build();
        SDWanProtos.Message resp = request.toBuilder()
                .setType(SDWanProtos.MsgTypeCode.RouteListRespType)
                .setData(routeList.toByteString())
                .build();
        channel.writeAndFlush(resp);
    }

    public void processNodeInfo(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.NodeInfoReq nodeInfoReq = SDWanProtos.NodeInfoReq.parseFrom(request.getData());
            Node onlineNode = nodeManager.getNodeMap().get(nodeInfoReq.getVip());
            SDWanProtos.NodeInfoResp.Builder builder = SDWanProtos.NodeInfoResp.newBuilder();
            if (null != onlineNode) {
                builder.setCode(0)
                        .setVip(onlineNode.getVip())
                        .setInternalAddr(SDWanProtos.SocketAddress.newBuilder()
                                .setIp(onlineNode.getInternalAddress().getHostString())
                                .setPort(onlineNode.getInternalAddress().getPort())
                                .build())
                        .setMappingType(SDWanProtos.MappingTypeCode.valueOf(onlineNode.getMappingType()))
                        .setPublicAddr(SDWanProtos.SocketAddress.newBuilder()
                                .setIp(onlineNode.getPublicAddress().getHostString())
                                .setPort(onlineNode.getPublicAddress().getPort())
                                .build())
                        .build();
            } else {
                builder.setCode(SDWanProtos.MessageCode.NotFound_VALUE);
            }
            SDWanProtos.NodeInfoResp nodeInfoResp = builder.build();
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.NodeInfoRespType)
                    .setData(nodeInfoResp.toByteString())
                    .build();
            channel.writeAndFlush(resp);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            SDWanProtos.NodeInfoResp nodeInfoResp = SDWanProtos.NodeInfoResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError_VALUE)
                    .build();
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.NodeInfoRespType)
                    .setData(nodeInfoResp.toByteString())
                    .build();
            channel.writeAndFlush(resp);
        }
    }
}

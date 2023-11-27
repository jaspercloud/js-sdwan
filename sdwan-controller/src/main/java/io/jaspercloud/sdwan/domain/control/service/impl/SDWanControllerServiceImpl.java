package io.jaspercloud.sdwan.domain.control.service.impl;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.config.SDWanControllerProperties;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.domain.control.entity.Node;
import io.jaspercloud.sdwan.domain.control.repository.NodeRepository;
import io.jaspercloud.sdwan.domain.control.repository.RouteRepository;
import io.jaspercloud.sdwan.domain.control.service.ConfigService;
import io.jaspercloud.sdwan.domain.control.service.SDWanControllerService;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.infra.AttributeKeys;
import io.jaspercloud.sdwan.domain.control.vo.NodeType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SDWanControllerServiceImpl implements SDWanControllerService, InitializingBean {

    @Resource
    private SDWanControllerProperties properties;

    @Resource
    private NodeRepository nodeRepository;

    @Resource
    private RouteRepository routeRepository;

    @Resource
    private ConfigService configService;

    @Resource
    private io.jaspercloud.sdwan.domain.control.service.SDWanNodeManager SDWanNodeManager;

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

    @Override
    public void regist(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.RegReq regReq = SDWanProtos.RegReq.parseFrom(request.getData());
            Node node = AttributeKeys.node(channel).get();
            if (null == node) {
                node = new Node();
                node.setMacAddress(regReq.getMacAddress());
                node.setNodeType(NodeType.valueOf(regReq.getNodeType().getNumber()));
                node.setAddressList(regReq.getAddressListList());
                bindVip(regReq, channel, node);
                AttributeKeys.node(channel).set(node);
                log.info("reg: nodeType={}, macAddr={}, vip={}, addressList={}",
                        node.getNodeType(),
                        node.getMacAddress(),
                        node.getVip(),
                        StringUtils.join(node.getAddressList()));
            }
            String vip = node.getVip();
            SDWanNodeManager.addChannel(vip, channel);
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
            log.error(e.getMessage(), e);
            SDWanProtos.RegResp regResp = SDWanProtos.RegResp.newBuilder()
                    .setCode(e.getCode())
                    .build();
            SDWanProtos.Message response = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.RegRespType)
                    .setData(regResp.toByteString())
                    .build();
            channel.writeAndFlush(response);
            channel.close();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
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

    @Override
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

    @Override
    public void processNodeInfo(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.NodeInfoReq nodeInfoReq = SDWanProtos.NodeInfoReq.parseFrom(request.getData());
            Node onlineNode = SDWanNodeManager.getNodeMap().get(nodeInfoReq.getVip());
            SDWanProtos.NodeInfoResp nodeInfoResp = SDWanProtos.NodeInfoResp.newBuilder()
                    .setCode(0)
                    .setVip(nodeInfoReq.getVip())
                    .addAllAddressList(null != onlineNode ? onlineNode.getAddressList() : Collections.emptyList())
                    .build();
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.NodeInfoRespType)
                    .setData(nodeInfoResp.toByteString())
                    .build();
            channel.writeAndFlush(resp);
        } catch (Throwable e) {
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

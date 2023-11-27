package io.jaspercloud.sdwan.domain.control.service.impl;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.adapter.controller.param.NodeDTO;
import io.jaspercloud.sdwan.adapter.controller.param.RouteDTO;
import io.jaspercloud.sdwan.adapter.server.RelayServer;
import io.jaspercloud.sdwan.config.SDWanControllerProperties;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.domain.control.entity.Node;
import io.jaspercloud.sdwan.domain.control.entity.Route;
import io.jaspercloud.sdwan.domain.control.repository.NodeRepository;
import io.jaspercloud.sdwan.domain.control.repository.RouteRepository;
import io.jaspercloud.sdwan.domain.control.service.ConfigService;
import io.jaspercloud.sdwan.domain.control.service.SDWanNodeManager;
import io.jaspercloud.sdwan.domain.control.vo.NodeType;
import io.jaspercloud.sdwan.exception.CidrParseException;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.infra.ErrorCode;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private SDWanControllerProperties properties;

    @Resource
    private NodeRepository nodeRepository;

    @Resource
    private RouteRepository routeRepository;

    @Resource
    private SDWanNodeManager sdWanNodeManager;

    @Resource
    private RelayServer relayServer;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void saveRoute(RouteDTO request) {
        transactionTemplate.executeWithoutResult(status -> {
            Node node = nodeRepository.queryById(request.getMeshId());
            if (null == node) {
                throw new ProcessCodeException(ErrorCode.NotFoundNode);
            }
            if (!NodeType.Mesh.equals(node.getNodeType())) {
                throw new ProcessCodeException(ErrorCode.NodeIsNotMesh);
            }
            try {
                Route route = new Route();
                route.setDestination(request.getDestination());
                route.setMeshId(node.getId());
                route.setRemark(request.getRemark());
                routeRepository.save(route);
            } catch (CidrParseException e) {
                throw new ProcessCodeException(ErrorCode.CidrError);
            }
        });
        //push
        pushRouteRuleList();
    }

    @Override
    public void deleteRoute(Long routeId) {
        transactionTemplate.executeWithoutResult(status -> {
            routeRepository.deleteById(routeId);
        });
        //push
        pushRouteRuleList();
    }

    @Override
    public void updateRoute(RouteDTO routeDTO) {
        transactionTemplate.executeWithoutResult(status -> {
            Route route = routeRepository.queryById(routeDTO.getId());
            if (null == route) {
                throw new ProcessCodeException(ErrorCode.NotFoundRoute);
            }
            if (null != routeDTO.getMeshId()) {
                route.setMeshId(routeDTO.getMeshId());
            }
            if (null != routeDTO.getDestination()) {
                route.setDestination(routeDTO.getDestination());
            }
            if (null != routeDTO.getRemark()) {
                route.setRemark(routeDTO.getRemark());
            }
            routeRepository.updateById(route);
        });
        //push
        pushRouteRuleList();
    }

    private void pushRouteRuleList() {
        List<SDWanProtos.Route> routes = getRouteList()
                .stream()
                .map(e -> SDWanProtos.Route.newBuilder()
                        .setDestination(e.getDestination())
                        .setNexthop(e.getNexthop())
                        .build())
                .collect(Collectors.toList());
        SDWanProtos.RouteList routeList = SDWanProtos.RouteList.newBuilder()
                .addAllRoute(routes)
                .build();
        SDWanProtos.Message response = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.RefreshRouteListType)
                .setData(routeList.toByteString())
                .build();
        List<Channel> channelList = sdWanNodeManager.getChannelList();
        for (Channel channel : channelList) {
            channel.writeAndFlush(response);
        }
    }

    @Override
    public List<RouteDTO> getRouteList() {
        List<RouteDTO> routeDTOList = new ArrayList<>();
        List<Route> routes = routeRepository.queryList();
        List<Long> nodeIdList = routes.stream().map(e -> e.getMeshId())
                .distinct()
                .collect(Collectors.toList());
        if (nodeIdList.isEmpty()) {
            return routeDTOList;
        }
        Map<Long, Node> nodeMap = nodeRepository.queryByIdList(nodeIdList)
                .stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (Route route : routes) {
            RouteDTO routeDTO = new RouteDTO();
            routeDTO.setId(route.getId());
            routeDTO.setMeshId(route.getMeshId());
            routeDTO.setDestination(route.getDestination());
            routeDTO.setNexthop(nodeMap.get(route.getMeshId()).getVip());
            routeDTO.setRemark(route.getRemark());
            routeDTOList.add(routeDTO);
        }
        return routeDTOList;
    }

    @Override
    public void updateNode(NodeDTO request) {
        Node node = nodeRepository.queryById(request.getId());
        if (null == node) {
            return;
        }
        if (!Cidr.contains(properties.getCidr(), request.getVip())) {
            throw new ProcessCodeException(ErrorCode.VipNotInCidr);
        }
        String curVIP = node.getVip();
        String newVIP = request.getVip();
        node.setVip(request.getVip());
        node.setRemark(request.getRemark());
        nodeRepository.updateById(node);
        if (!StringUtils.equals(curVIP, newVIP)) {
            sdWanNodeManager.deleteChannel(curVIP);
        }
    }

    @Override
    public void deleteNode(Long id) {
        Node node = nodeRepository.queryById(id);
        if (null == node) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            long count = routeRepository.countByMeshId(id);
            if (count > 0) {
                throw new ProcessCodeException(ErrorCode.MeshUsed);
            }
            nodeRepository.deleteById(id);
        });
        sdWanNodeManager.deleteChannel(node.getVip());
    }

    @Override
    public List<NodeDTO> getNodeList() {
        Map<String, Node> onlineMap = sdWanNodeManager.getNodeMap();
        List<Node> nodeList = nodeRepository.queryList();
        List<NodeDTO> resultList = nodeList.stream().map(e -> {
            NodeDTO nodeDTO = new NodeDTO();
            nodeDTO.setId(e.getId());
            nodeDTO.setNodeType(e.getNodeType());
            nodeDTO.setVip(e.getVip());
            nodeDTO.setMacAddress(e.getMacAddress());
            nodeDTO.setRemark(e.getRemark());
            Node node = onlineMap.get(e.getVip());
            nodeDTO.setOnline(null != node);
            if (null != node) {
                nodeDTO.setAddressList(node.getAddressList());
            }
            return nodeDTO;
        }).collect(Collectors.toList());
        return resultList;
    }

    @Override
    public List<NodeDTO> getMeshNodeList() {
        List<Node> nodeList = nodeRepository.getMeshNodeList();
        List<NodeDTO> resultList = nodeList.stream().map(e -> {
            NodeDTO nodeDTO = new NodeDTO();
            nodeDTO.setId(e.getId());
            nodeDTO.setNodeType(e.getNodeType());
            nodeDTO.setVip(e.getVip());
            nodeDTO.setMacAddress(e.getMacAddress());
            nodeDTO.setRemark(e.getRemark());
            return nodeDTO;
        }).collect(Collectors.toList());
        return resultList;
    }

    @Override
    public void disconnectNode(Long id) {
        Node node = nodeRepository.queryById(id);
        if (null == node) {
            return;
        }
        sdWanNodeManager.deleteChannel(node.getVip());
    }
}

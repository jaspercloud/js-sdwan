package io.jaspercloud.sdwan.app;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.domian.Node;
import io.jaspercloud.sdwan.domian.Route;
import io.jaspercloud.sdwan.exception.CidrParseException;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.infra.NodeRepository;
import io.jaspercloud.sdwan.infra.RouteRepository;
import io.jaspercloud.sdwan.support.NodeManager;
import io.jaspercloud.sdwan.support.SDWanControllerProperties;
import io.netty.channel.Channel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private NodeManager nodeManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void saveRoute(RouteDTO request) {
        transactionTemplate.executeWithoutResult(status -> {
            Node node = nodeRepository.queryById(request.getMeshId());
            if (null == node) {
                throw new ProcessCodeException(ErrorCode.NotFoundNode);
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
                .setType(SDWanProtos.MsgTypeCode.RefreshRouteList)
                .setData(routeList.toByteString())
                .build();
        List<Channel> channelList = nodeManager.getChannelList();
        for (Channel channel : channelList) {
            channel.writeAndFlush(response);
        }
    }

    @Override
    public List<RouteDTO> getRouteList() {
        List<RouteDTO> routeDTOList = new ArrayList<>();
        List<Route> routes = routeRepository.queryList();
        List<Long> nodeIdList = routes.stream().map(e -> e.getMeshId())
                .collect(Collectors.toList());
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
    public void saveNode(NodeDTO request) {
        transactionTemplate.executeWithoutResult(status -> {
            boolean contains = Cidr.contains(properties.getCidr(), request.getVip());
            if (!contains) {
                throw new ProcessCodeException(ErrorCode.IpNotInCidr);
            }
            Node queryVip = nodeRepository.queryByVip(request.getVip());
            if (null != queryVip) {
                throw new ProcessCodeException(ErrorCode.NodeVipExist);
            }
            Node queryMac = nodeRepository.queryByMacAddress(request.getMacAddress());
            if (null != queryMac) {
                throw new ProcessCodeException(ErrorCode.NodeMacExist);
            }
            Node node = new Node();
            node.setVip(request.getVip());
            node.setMacAddress(request.getMacAddress());
            node.setRemark(request.getRemark());
            nodeRepository.save(node);
        });
    }

    @Override
    public void deleteNode(Long id) {
        transactionTemplate.executeWithoutResult(status -> {
            long count = routeRepository.countByMeshId(id);
            if (count > 0) {
                throw new ProcessCodeException(ErrorCode.MeshUsed);
            }
            nodeRepository.deleteById(id);
        });
    }
}

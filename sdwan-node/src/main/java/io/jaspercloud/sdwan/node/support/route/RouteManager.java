package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.support.node.SDWanDataHandler;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public abstract class RouteManager implements InitializingBean {

    protected AtomicReference<List<SDWanProtos.Route>> cache = new AtomicReference<>(Collections.emptyList());

    private SDWanNode sdWanNode;
    private ConnectionManager connectionManager;

    private List<UpdateRouteHandler> handlerList = new ArrayList<>();

    public void addUpdateRouteHandler(UpdateRouteHandler updateRouteHandler) {
        handlerList.add(updateRouteHandler);
    }

    public RouteManager(SDWanNode sdWanNode, ConnectionManager connectionManager) {
        this.sdWanNode = sdWanNode;
        this.connectionManager = connectionManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sdWanNode.addDataHandler(new SDWanDataHandler() {
            @Override
            public void onData(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
                try {
                    if (SDWanProtos.MsgTypeCode.RefreshRouteListType.equals(msg.getType())) {
                        for (UpdateRouteHandler handler : handlerList) {
                            List<SDWanProtos.Route> routeList = SDWanProtos.RouteList.parseFrom(msg.getData())
                                    .getRouteList();
                            handler.onUpdate(routeList);
                        }
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    public void route(String localVIP, SDWanProtos.IpPacket ipPacket) {
        SDWanProtos.Route route = findRoute(ipPacket.getDstIP());
        if (null != route) {
            SDWanProtos.RoutePacket routePacket = SDWanProtos.RoutePacket.newBuilder()
                    .setSrcVIP(localVIP)
                    .setDstVIP(route.getNexthop())
                    .setPayload(ipPacket)
                    .build();
            connectionManager.send(routePacket);
        } else {
            SDWanProtos.RoutePacket routePacket = SDWanProtos.RoutePacket.newBuilder()
                    .setSrcVIP(localVIP)
                    .setDstVIP(ipPacket.getDstIP())
                    .setPayload(ipPacket)
                    .build();
            connectionManager.send(routePacket);
        }
    }

    private SDWanProtos.Route findRoute(String dstIP) {
        for (SDWanProtos.Route route : cache.get()) {
            if (Cidr.contains(route.getDestination(), dstIP)) {
                return route;
            }
        }
        return null;
    }

    public void initRoute(TunChannel tunChannel) throws Exception {
        SDWanProtos.RouteList routeList = sdWanNode.getRouteList().get();
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        String vip = tunAddress.getVip();
        List<SDWanProtos.Route> newList = routeList.getRouteList()
                .stream()
                .filter(e -> !StringUtils.equals(e.getNexthop(), vip))
                .collect(Collectors.toList());
        doUpdateRouteList(tunChannel, cache.get(), newList);
        cache.set(newList);
    }

    public void updateRouteList(TunChannel tunChannel, List<SDWanProtos.Route> routeList) {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        String vip = tunAddress.getVip();
        List<SDWanProtos.Route> newList = routeList.stream()
                .filter(e -> !StringUtils.equals(e.getNexthop(), vip))
                .collect(Collectors.toList());
        doUpdateRouteList(tunChannel, cache.get(), newList);
        cache.set(newList);
    }

    public void releaseRoute(TunChannel tunChannel) {
        List<SDWanProtos.Route> newList = Collections.emptyList();
        doUpdateRouteList(tunChannel, cache.get(), newList);
        cache.set(newList);
    }

    private void doUpdateRouteList(TunChannel tunChannel, List<SDWanProtos.Route> oldList, List<SDWanProtos.Route> newList) {
        for (SDWanProtos.Route route : oldList) {
            try {
                log.info("deleteRoute: destination={}, nexthop={}", route.getDestination(), route.getNexthop());
                deleteRoute(tunChannel, route);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
        for (SDWanProtos.Route route : newList) {
            try {
                log.info("addRoute: destination={}, nexthop={}", route.getDestination(), route.getNexthop());
                addRoute(tunChannel, route);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected abstract void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception;

    protected abstract void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception;
}

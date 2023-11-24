package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class RouteManager {

    protected AtomicReference<List<SDWanProtos.Route>> cache = new AtomicReference<>(Collections.emptyList());

    private SDWanNode sdWanNode;
    private ConnectionManager connectionManager;

    public RouteManager(SDWanNode sdWanNode, ConnectionManager connectionManager) {
        this.sdWanNode = sdWanNode;
        this.connectionManager = connectionManager;
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

    protected abstract void doUpdateRouteList(TunChannel tunChannel, List<SDWanProtos.Route> oldList, List<SDWanProtos.Route> newList) throws Exception;

    public abstract void releaseRoute(TunChannel tunChannel, List<SDWanProtos.Route> routeList) throws Exception;
}

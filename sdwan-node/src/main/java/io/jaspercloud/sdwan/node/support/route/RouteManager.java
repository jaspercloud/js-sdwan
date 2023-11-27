package io.jaspercloud.sdwan.node.support.route;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
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
    private String cidr;

    private SDWanNode sdWanNode;

    private List<UpdateRouteHandler> handlerList = new ArrayList<>();
    private List<RouteChain> routeChainList = new ArrayList<>();

    public void addUpdateRouteHandler(UpdateRouteHandler updateRouteHandler) {
        handlerList.add(updateRouteHandler);
    }

    public void addRouteChain(RouteChain chain) {
        routeChainList.add(chain);
    }

    public RouteManager(SDWanNode sdWanNode) {
        this.sdWanNode = sdWanNode;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sdWanNode.addDataHandler((ctx, msg) -> {
            try {
                switch (msg.getType().getNumber()) {
                    case SDWanProtos.MsgTypeCode.RefreshRouteListType_VALUE: {
                        for (UpdateRouteHandler handler : handlerList) {
                            List<SDWanProtos.Route> routeList = SDWanProtos.RouteList.parseFrom(msg.getData())
                                    .getRouteList();
                            handler.onUpdate(routeList);
                        }
                        break;
                    }
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    public SDWanProtos.RoutePacket routeOut(String localVIP, SDWanProtos.IpPacket ipPacket) {
        for (RouteChain chain : routeChainList) {
            ipPacket = chain.routeOut(ipPacket);
        }
        if (null == ipPacket) {
            return null;
        }
        SDWanProtos.Route route = findRoute(ipPacket.getDstIP());
        SDWanProtos.RoutePacket routePacket;
        if (null != route) {
            routePacket = SDWanProtos.RoutePacket.newBuilder()
                    .setSrcVIP(localVIP)
                    .setDstVIP(route.getNexthop())
                    .setPayload(ipPacket)
                    .build();
        } else if (StringUtils.isNotEmpty(cidr) && Cidr.contains(cidr, ipPacket.getDstIP())) {
            routePacket = SDWanProtos.RoutePacket.newBuilder()
                    .setSrcVIP(localVIP)
                    .setDstVIP(ipPacket.getDstIP())
                    .setPayload(ipPacket)
                    .build();
        } else {
            return null;
        }
        return routePacket;
    }

    public SDWanProtos.IpPacket routeIn(SDWanProtos.RoutePacket routePacket) {
        SDWanProtos.IpPacket ipPacket = routePacket.getPayload();
        for (RouteChain chain : routeChainList) {
            ipPacket = chain.routeIn(ipPacket);
        }
        if (null == ipPacket) {
            return null;
        }
        SDWanProtos.Route route = findRoute(ipPacket.getDstIP());
        if (null != route) {
            return ipPacket;
        } else if (StringUtils.isNotEmpty(cidr) && Cidr.contains(cidr, ipPacket.getDstIP())) {
            return ipPacket;
        } else {
            return null;
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
        int maskBits = tunAddress.getMaskBits();
        cidr = Cidr.parseCidr(vip, maskBits);
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
        cidr = null;
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

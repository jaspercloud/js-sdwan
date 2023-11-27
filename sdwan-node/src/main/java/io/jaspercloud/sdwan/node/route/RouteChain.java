package io.jaspercloud.sdwan.node.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

public interface RouteChain {

    SDWanProtos.IpPacket routeOut(SDWanProtos.IpPacket ipPacket);

    SDWanProtos.IpPacket routeIn(SDWanProtos.IpPacket ipPacket);
}

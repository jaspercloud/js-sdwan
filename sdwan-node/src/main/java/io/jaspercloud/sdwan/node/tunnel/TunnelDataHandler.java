package io.jaspercloud.sdwan.node.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

@FunctionalInterface
public interface TunnelDataHandler {

    void onData(DataTunnel dataTunnel, SDWanProtos.RoutePacket routePacket);
}

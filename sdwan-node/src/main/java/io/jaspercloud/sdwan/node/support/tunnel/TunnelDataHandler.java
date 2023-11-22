package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

public interface TunnelDataHandler {

    void onData(DataTunnel dataTunnel, SDWanProtos.RoutePacket routePacket);
}

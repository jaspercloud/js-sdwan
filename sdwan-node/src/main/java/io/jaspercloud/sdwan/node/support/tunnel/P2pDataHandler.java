package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

public interface P2pDataHandler {

    void onData(DataTunnel dataTunnel, SDWanProtos.RoutePacket routePacket);
}

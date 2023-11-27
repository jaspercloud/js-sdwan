package io.jaspercloud.sdwan.node.connection;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

@FunctionalInterface
public interface ConnectionDataHandler {

    void onData(PeerConnection connection, SDWanProtos.RoutePacket packet);
}

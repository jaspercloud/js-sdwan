package io.jaspercloud.sdwan.node.support.connection;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

public interface ConnectionDataHandler {

    void onData(PeerConnection connection, SDWanProtos.IpPacket packet);
}
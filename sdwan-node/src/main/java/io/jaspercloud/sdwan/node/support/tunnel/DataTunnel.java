package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.detection.DetectionInfo;
import io.jaspercloud.sdwan.stun.StunPacket;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DataTunnel {

    DetectionInfo getDetectionInfo();

    void addCloseListener(Consumer<DataTunnel> consumer);

    void close();

    CompletableFuture<StunPacket> check();

    void send(SDWanProtos.RoutePacket routePacket);

    SDWanProtos.RoutePacket receive(SDWanProtos.P2pPacket p2pPacket);
}

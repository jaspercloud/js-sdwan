package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PeerConnection {

    private DataTunnel dataTunnel;

    private PeerConnection(DataTunnel dataTunnel) {
        this.dataTunnel = dataTunnel;
    }

    public static CompletableFuture<PeerConnection> create(P2pManager p2pManager,
                                                           String srcVIP,
                                                           String dstVIP,
                                                           List<String> addressList) {
        return p2pManager.create(srcVIP, dstVIP, addressList)
                .thenApply(tunnel -> {
                    return new PeerConnection(tunnel);
                });
    }

    public void send(SDWanProtos.RoutePacket routePacket) {
        dataTunnel.send(routePacket);
    }
}

package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.stun.StunClient;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class PeerConnection {

    private DataTunnel dataTunnel;

    public PeerConnection(DataTunnel dataTunnel) {
        this.dataTunnel = dataTunnel;
    }

    public static CompletableFuture<PeerConnection> create(Config config) {
        //try internalAddr
        return config.getP2pManager()
                .punch(config.getSrcInternalAddr(), config.getDstInternalAddr())
                .thenApply(address -> CompletableFuture.completedFuture(address))
                .exceptionally(throwable -> {
                    //try publicAddr
                    return config.getP2pManager()
                            .punch(config.getSrcPublicAddr(), config.getDstPublicAddr());
                })
                .thenCompose(f -> f)
                .thenApply(address -> {
                    DataTunnel dataTunnel = new PunchDataTunnel(config.getStunClient(), address);
                    config.getP2pManager().addTunnel(address, dataTunnel);
                    return CompletableFuture.completedFuture(dataTunnel);
                })
                .exceptionally(throwable -> {
                    //try relay
                    return config.getRelayManager()
                            .check(config.getRelayToken())
                            .thenApply(result -> {
                                if (!result) {
                                    throw new ProcessException("create peerConnection failed");
                                }
                                DataTunnel dataTunnel = new RelayDataTunnel(config.getStunClient(), config.getRelayAddr(), config.getRelayToken());
                                config.getRelayManager().addTunnel(config.getRelayToken(), dataTunnel);
                                return dataTunnel;
                            });
                })
                .thenCompose(f -> f)
                .thenApply(dataTunnel -> new PeerConnection(dataTunnel));
    }

    public void send(SDWanProtos.RoutePacket routePacket) {
        dataTunnel.send(routePacket);
    }

    @Data
    public static class Config {

        private SDWanNode sdWanNode;
        private StunClient stunClient;
        private P2pManager p2pManager;
        private RelayManager relayManager;
        private InetSocketAddress srcInternalAddr;
        private InetSocketAddress srcPublicAddr;
        private InetSocketAddress dstInternalAddr;
        private InetSocketAddress dstPublicAddr;
        private InetSocketAddress relayAddr;
        private String relayToken;
    }
}

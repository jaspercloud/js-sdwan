package io.jaspercloud.sdwan.node.support.connection;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.tunnel.DataTunnel;
import io.jaspercloud.sdwan.node.support.tunnel.P2pManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PeerConnection {

    private DataTunnel dataTunnel;
    private CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public void addCloseListener(Consumer<PeerConnection> consumer) {
        closeFuture.thenAccept(v -> {
            consumer.accept(this);
        });
    }

    private PeerConnection(DataTunnel dataTunnel) {
        this.dataTunnel = dataTunnel;
    }

    public void close() {
        closeFuture.complete(null);
    }

    public static PeerConnection create(DataTunnel dataTunnel) {
        return new PeerConnection(dataTunnel);
    }

    public static CompletableFuture<PeerConnection> create(P2pManager p2pManager,
                                                           String srcVIP,
                                                           String dstVIP,
                                                           List<String> addressList) {
        return p2pManager.create(srcVIP, dstVIP, addressList)
                .thenApply(tunnel -> {
                    PeerConnection connection = new PeerConnection(tunnel);
                    tunnel.addCloseListener(new Consumer<DataTunnel>() {
                        @Override
                        public void accept(DataTunnel dataTunnel) {
                            connection.close();
                        }
                    });
                    return connection;
                });
    }

    public void send(SDWanProtos.RoutePacket routePacket) {
        dataTunnel.send(routePacket);
    }
}

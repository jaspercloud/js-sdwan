package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class P2pDataTunnel implements DataTunnel {

    private StunClient stunClient;
    private InetSocketAddress address;
    private CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public P2pDataTunnel(StunClient stunClient, InetSocketAddress address) {
        this.stunClient = stunClient;
        this.address = address;
    }

    @Override
    public void addCloseListener(Consumer<DataTunnel> consumer) {
        closeFuture.thenAccept(v -> {
            consumer.accept(this);
        });
    }

    @Override
    public void close() {
        closeFuture.complete(null);
    }

    @Override
    public CompletableFuture<Boolean> check() {
        return stunClient.sendHeart(address)
                .handle((stunPacket, throwable) -> {
                    if (null != throwable) {
                        return false;
                    }
                    return true;
                });
    }

    @Override
    public void send(SDWanProtos.RoutePacket routePacket) {
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.Data, new BytesAttr(routePacket.toByteArray()));
        stunClient.send(address, message);
    }
}

package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.detection.DetectionInfo;
import io.jaspercloud.sdwan.node.support.node.RelayClient;
import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class RelayDataTunnel implements DataTunnel {

    private StunClient stunClient;
    private RelayClient relayClient;
    private DetectionInfo detectionInfo;
    private InetSocketAddress relayAddr;
    private String relayToken;
    private CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public RelayDataTunnel(StunClient stunClient, RelayClient relayClient, DetectionInfo detectionInfo, InetSocketAddress relayAddr, String relayToken) {
        this.stunClient = stunClient;
        this.relayClient = relayClient;
        this.detectionInfo = detectionInfo;
        this.relayAddr = relayAddr;
        this.relayToken = relayToken;
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
    public CompletableFuture<StunPacket> check() {
        return relayClient.sendHeart(relayAddr, relayToken);
    }

    @Override
    public void send(SDWanProtos.RoutePacket routePacket) {
        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.newBuilder()
                .setSrcAddress(detectionInfo.getSrcAddress())
                .setDstAddress(detectionInfo.getDstAddress())
                .setPayload(routePacket)
                .build();
        System.out.println(String.format("p2pPacket: src=%s, dst=%s", p2pPacket.getSrcAddress(), p2pPacket.getDstAddress()));
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.RelayToken, new StringAttr(relayToken));
        message.getAttrs().put(AttrType.Data, new BytesAttr(p2pPacket.toByteArray()));
        stunClient.send(relayAddr, message);
    }
}

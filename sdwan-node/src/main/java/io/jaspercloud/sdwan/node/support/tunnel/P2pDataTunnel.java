package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.detection.DetectionInfo;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.BytesAttr;
import io.jaspercloud.sdwan.stun.MessageType;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.stun.StunMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class P2pDataTunnel implements DataTunnel {

    private StunClient stunClient;
    private DetectionInfo detectionInfo;
    private InetSocketAddress address;
    private CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public P2pDataTunnel(StunClient stunClient, DetectionInfo detectionInfo, InetSocketAddress address) {
        this.stunClient = stunClient;
        this.detectionInfo = detectionInfo;
        this.address = address;
    }

    @Override
    public DetectionInfo getDetectionInfo() {
        return detectionInfo;
    }

    @Override
    public void addCloseListener(Consumer<DataTunnel> consumer) {
        closeFuture.thenAccept(v -> {
            log.info("tunnel onClose: {}", toString());
            consumer.accept(this);
        });
    }

    @Override
    public void close() {
        log.info("tunnel close: {}", toString());
        closeFuture.complete(null);
    }

    @Override
    public CompletableFuture<Boolean> check() {
        return stunClient.sendBind(address)
                .handle((result, error) -> {
                    return null == error;
                });
    }

    @Override
    public void send(SDWanProtos.RoutePacket routePacket) {
        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.newBuilder()
                .setSrcAddress(detectionInfo.getSrcAddress())
                .setDstAddress(detectionInfo.getDstAddress())
                .setPayload(routePacket)
                .build();
        String src = UriComponentsBuilder.fromUriString(p2pPacket.getSrcAddress())
                .build().getHost();
        String dst = UriComponentsBuilder.fromUriString(p2pPacket.getDstAddress())
                .build().getHost();
        log.debug("P2pDataTunnel p2pPacket send: src={}, dst={}", src, dst);
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.Data, new BytesAttr(p2pPacket.toByteArray()));
        stunClient.send(address, message);
    }

    @Override
    public SDWanProtos.RoutePacket receive(SDWanProtos.P2pPacket p2pPacket) {
        String src = UriComponentsBuilder.fromUriString(p2pPacket.getSrcAddress())
                .build().getHost();
        String dst = UriComponentsBuilder.fromUriString(p2pPacket.getDstAddress())
                .build().getHost();
        log.debug("P2pDataTunnel p2pPacket recv: src={}, dst={}", src, dst);
        SDWanProtos.RoutePacket routePacket = p2pPacket.getPayload();
        return routePacket;
    }

    @Override
    public String toString() {
        String src = UriComponentsBuilder.fromUriString(detectionInfo.getSrcAddress())
                .build().getHost();
        String dst = UriComponentsBuilder.fromUriString(detectionInfo.getDstAddress())
                .build().getHost();
        return String.format("%s -> %s", src, dst);
    }
}

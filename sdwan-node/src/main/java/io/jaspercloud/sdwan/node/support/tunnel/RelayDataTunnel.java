package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.detection.AddressType;
import io.jaspercloud.sdwan.node.support.detection.DetectionInfo;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.BytesAttr;
import io.jaspercloud.sdwan.stun.MessageType;
import io.jaspercloud.sdwan.stun.StringAttr;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.stun.StunMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class RelayDataTunnel implements DataTunnel {

    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private String vip;
    private DetectionInfo detectionInfo;
    private InetSocketAddress relayAddr;
    private String relayToken;
    private CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    public RelayDataTunnel(SDWanNode sdWanNode, StunClient stunClient, String vip, DetectionInfo detectionInfo, InetSocketAddress relayAddr, String relayToken) {
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.vip = vip;
        this.detectionInfo = detectionInfo;
        this.relayAddr = relayAddr;
        this.relayToken = relayToken;
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
        return sdWanNode.queryNodeInfo(vip)
                .handle((result, error) -> {
                    if (null != error) {
                        return false;
                    }
                    if (SDWanProtos.MessageCode.Success_VALUE != result.getCode()) {
                        return false;
                    }
                    long count = result.getAddressListList().stream()
                            .map(e -> UriComponentsBuilder.fromUriString(e).build())
                            .filter(e -> AddressType.RELAY.equals(e.getScheme()))
                            .map(e -> e.getQueryParams().getFirst("token"))
                            .filter(token -> StringUtils.equals(token, relayToken))
                            .count();
                    return count > 0;
                });
    }

    @Override
    public void send(SDWanProtos.RoutePacket routePacket) {
        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.newBuilder().setSrcAddress(detectionInfo.getSrcAddress()).setDstAddress(detectionInfo.getDstAddress()).setPayload(routePacket).build();
        String src = UriComponentsBuilder.fromUriString(p2pPacket.getSrcAddress()).build().getQueryParams().getFirst("token");
        String dst = UriComponentsBuilder.fromUriString(p2pPacket.getDstAddress()).build().getQueryParams().getFirst("token");
        log.debug("RelayDataTunnel p2pPacket send: src={}, dst={}", src, dst);
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.RelayToken, new StringAttr(relayToken));
        message.getAttrs().put(AttrType.Data, new BytesAttr(p2pPacket.toByteArray()));
        stunClient.send(relayAddr, message);
    }

    @Override
    public SDWanProtos.RoutePacket receive(SDWanProtos.P2pPacket p2pPacket) {
        String src = UriComponentsBuilder.fromUriString(p2pPacket.getSrcAddress()).build().getQueryParams().getFirst("token");
        String dst = UriComponentsBuilder.fromUriString(p2pPacket.getDstAddress()).build().getQueryParams().getFirst("token");
        log.debug("RelayDataTunnel p2pPacket recv: src={}, dst={}", src, dst);
        SDWanProtos.RoutePacket routePacket = p2pPacket.getPayload();
        return routePacket;
    }

    @Override
    public String toString() {
        String src = UriComponentsBuilder.fromUriString(detectionInfo.getSrcAddress())
                .build().getQueryParams().getFirst("token");
        String dst = UriComponentsBuilder.fromUriString(detectionInfo.getDstAddress())
                .build().getQueryParams().getFirst("token");
        return String.format("%s -> %s", src, dst);
    }
}

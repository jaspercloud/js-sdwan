package io.jaspercloud.sdwan.node.detection;

import io.jaspercloud.sdwan.node.node.RelayClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class RelayP2pDetection implements P2pDetection {

    private RelayClient relayClient;

    public RelayP2pDetection(RelayClient relayClient) {
        this.relayClient = relayClient;
    }

    @Override
    public String type() {
        return AddressType.RELAY;
    }

    @Override
    public CompletableFuture<DetectionInfo> detection(String uri) {
        InetSocketAddress relayAddress = relayClient.getRelayAddress();
        String relayToken = relayClient.getRelayToken();
        String selfUri = UriComponentsBuilder.newInstance()
                .scheme(AddressType.RELAY)
                .host(relayAddress.getHostString())
                .port(relayAddress.getPort())
                .queryParam("token", relayToken)
                .build().toString();
        return CompletableFuture.completedFuture(new DetectionInfo(selfUri, uri));
    }
}

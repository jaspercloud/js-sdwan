package io.jaspercloud.sdwan.node.support.detection;

import java.util.concurrent.CompletableFuture;

public class RelayP2pDetection implements P2pDetection {

    public RelayP2pDetection() {
    }

    @Override
    public String type() {
        return AddressType.RELAY;
    }

    @Override
    public CompletableFuture<String> detection(String uri) {
        return CompletableFuture.completedFuture(uri);
    }
}

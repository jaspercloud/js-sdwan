package io.jaspercloud.sdwan.node.support.detection;

import java.util.concurrent.CompletableFuture;

public interface P2pDetection {

    String type();

    CompletableFuture<String> detection(String uri);
}

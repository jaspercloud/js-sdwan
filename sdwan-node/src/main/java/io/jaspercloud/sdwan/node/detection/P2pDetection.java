package io.jaspercloud.sdwan.node.detection;

import java.util.concurrent.CompletableFuture;

public interface P2pDetection {

    String type();

    CompletableFuture<DetectionInfo> detection(String uri);
}

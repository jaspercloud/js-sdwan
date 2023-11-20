package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DataTunnel {

    void addCloseListener(Consumer<DataTunnel> consumer);

    void close();

    CompletableFuture<Boolean> check();

    void send(SDWanProtos.RoutePacket routePacket);
}

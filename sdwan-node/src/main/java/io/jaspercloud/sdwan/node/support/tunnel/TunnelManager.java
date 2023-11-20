package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.MappingManager;
import io.jaspercloud.sdwan.node.support.SDWanNode;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TunnelManager {

    private Map<String, PeerConnection> connectionMap = new ConcurrentHashMap<>();
    private SDWanNode sdWanNode;
    private MappingManager mappingManager;
    private P2pManager p2pManager;
    private RelayManager relayManager;

    public TunnelManager(SDWanNode sdWanNode, MappingManager mappingManager, P2pManager p2pManager, RelayManager relayManager) {
        this.sdWanNode = sdWanNode;
        this.mappingManager = mappingManager;
        this.p2pManager = p2pManager;
        this.relayManager = relayManager;
    }

    public CompletableFuture<PeerConnection> getConnection(String vip) {
        PeerConnection target = connectionMap.get(vip);
        if (null != target) {
            return CompletableFuture.completedFuture(target);
        }
        CompletableFuture future = new CompletableFuture();
        sdWanNode.queryNodeInfo(vip)
                .whenComplete((resp, nodeInfoError) -> {
                    if (null != nodeInfoError) {
                        future.completeExceptionally(nodeInfoError);
                        return;
                    }
                    if (SDWanProtos.MessageCode.Success_VALUE != resp.getCode()) {
                        future.completeExceptionally(new ProcessException("query nodeInfo error"));
                        return;
                    }
                    PeerConnection.Config config = new PeerConnection.Config();
                    config.setSdWanNode(sdWanNode);
                    config.setP2pManager(p2pManager);
                    config.setRelayManager(relayManager);
                    config.setSrcInternalAddr((InetSocketAddress) sdWanNode.getChannel().localAddress());
                    config.setDstInternalAddr(config.getDstInternalAddr());
                    config.setSrcPublicAddr(mappingManager.getMappingAddress().getMappingAddress());
                    config.setDstPublicAddr(config.getDstPublicAddr());
                    config.setRelayToken(config.getRelayToken());
                    PeerConnection.create(config)
                            .whenComplete((connection, connectionError) -> {
                                if (null != connectionError) {
                                    future.completeExceptionally(connectionError);
                                    return;
                                }
                                connectionMap.put(vip, connection);
                                future.complete(connection);
                            });
                });
        return future;
    }

    public void send(SDWanProtos.RoutePacket routePacket) {
        getConnection(routePacket.getDstVIP())
                .thenAccept(connection -> {
                    connection.send(routePacket);
                });
    }
}

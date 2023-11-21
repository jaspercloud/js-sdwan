package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.MappingManager;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.StunClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TunnelManager {

    private Map<String, PeerConnection> connectionMap = new ConcurrentHashMap<>();
    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private MappingManager mappingManager;
    private P2pManager p2pManager;
    private RelayManager relayManager;

    public TunnelManager(SDWanNodeProperties properties, SDWanNode sdWanNode, StunClient stunClient, MappingManager mappingManager, P2pManager p2pManager, RelayManager relayManager) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
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
                    config.setStunClient(stunClient);
                    config.setMappingManager(mappingManager);
                    config.setP2pManager(p2pManager);
                    config.setRelayManager(relayManager);
                    config.setAddressList(resp.getAddressListList());
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

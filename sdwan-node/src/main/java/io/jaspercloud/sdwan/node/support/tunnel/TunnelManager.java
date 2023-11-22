package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.MappingManager;
import io.jaspercloud.sdwan.node.support.RelayClient;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.AddressAttr;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.jaspercloud.sdwan.stun.StunClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TunnelManager implements InitializingBean {

    private Map<String, PeerConnection> connectionMap = new ConcurrentHashMap<>();
    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private RelayClient relayClient;
    private MappingManager mappingManager;
    private P2pManager p2pManager;

    private List<ConnectionDataHandler> connectionDataHandlerList = new ArrayList<>();

    public void addConnectionDataHandler(ConnectionDataHandler handler) {
        connectionDataHandlerList.add(handler);
    }

    public TunnelManager(SDWanNodeProperties properties, SDWanNode sdWanNode, StunClient stunClient, RelayClient relayClient, MappingManager mappingManager, P2pManager p2pManager) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.relayClient = relayClient;
        this.mappingManager = mappingManager;
        this.p2pManager = p2pManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        p2pManager.addTunnelDataHandler(new TunnelDataHandler() {
            @Override
            public void onData(DataTunnel dataTunnel, SDWanProtos.RoutePacket routePacket) {
                PeerConnection connection = connectionMap.get(routePacket.getDstVIP());
                if (null == connection) {
                    connection = PeerConnection.create(dataTunnel);
                    connectionMap.put(routePacket.getDstVIP(), connection);
                }
                SDWanProtos.IpPacket ipPacket = routePacket.getPayload();
                for (ConnectionDataHandler handler : connectionDataHandlerList) {
                    handler.onData(connection, ipPacket);
                }
            }
        });
    }

    public CompletableFuture<PeerConnection> getConnection(String srcVIP, String dstVIP) {
        PeerConnection target = connectionMap.get(dstVIP);
        if (null != target) {
            return CompletableFuture.completedFuture(target);
        }
        CompletableFuture future = new CompletableFuture();
        sdWanNode.queryNodeInfo(dstVIP)
                .whenComplete((resp, nodeInfoError) -> {
                    if (null != nodeInfoError) {
                        future.completeExceptionally(nodeInfoError);
                        return;
                    }
                    if (SDWanProtos.MessageCode.Success_VALUE != resp.getCode()) {
                        future.completeExceptionally(new ProcessException("query nodeInfo error"));
                        return;
                    }
                    //address
                    InetSocketAddress sdWanNodeLocalAddress = (InetSocketAddress) sdWanNode.getChannel().localAddress();
                    InetSocketAddress stunClientLocalAddress = (InetSocketAddress) stunClient.getChannel().localAddress();
                    String host = UriComponentsBuilder.newInstance()
                            .scheme("host")
                            .host(sdWanNodeLocalAddress.getHostString())
                            .port(stunClientLocalAddress.getPort())
                            .build().toString();
                    MappingAddress mappingAddress = mappingManager.getMappingAddress();
                    String srflx = UriComponentsBuilder.newInstance()
                            .scheme("srflx")
                            .host(mappingAddress.getMappingAddress().getHostString())
                            .port(mappingAddress.getMappingAddress().getPort())
                            .queryParam("mappingType", mappingAddress.getMappingType().name())
                            .build().toString();
                    String relay = UriComponentsBuilder.newInstance()
                            .scheme("relay")
                            .host(properties.getRelayServer().getHostString())
                            .port(properties.getRelayServer().getPort())
                            .queryParam("token", relayClient.getRelayToken())
                            .build().toString();
                    //try target bind
                    Map<String, UriComponents> uriComponentsMap = resp.getAddressListList().stream()
                            .map(uri -> UriComponentsBuilder.fromUriString(uri).build())
                            .collect(Collectors.toMap(e -> e.getScheme(), e -> e));
                    UriComponents components = uriComponentsMap.get("srflx");
                    stunClient.sendBind(new InetSocketAddress(components.getHost(), components.getPort()))
                            .handle((bindResp, bindError) -> {
                                if (null != bindError) {
                                    return Arrays.asList(host, srflx, relay);
                                }
                                AddressAttr mappedAddressAttr = bindResp.content().getAttr(AttrType.MappedAddress);
                                InetSocketAddress punchAddress = mappedAddressAttr.getAddress();
                                String prflx = UriComponentsBuilder.newInstance()
                                        .scheme("prflx")
                                        .host(punchAddress.getHostString())
                                        .port(punchAddress.getPort())
                                        .build().toString();
                                return Arrays.asList(host, srflx, prflx, relay);
                            })
                            .thenAccept(addressList -> {
                                PeerConnection.create(p2pManager, srcVIP, dstVIP, addressList)
                                        .whenComplete((connection, connectionError) -> {
                                            if (null != connectionError) {
                                                future.completeExceptionally(connectionError);
                                                return;
                                            }
                                            connectionMap.put(dstVIP, connection);
                                            future.complete(connection);
                                        });
                            });
                });
        return future;
    }

    public void send(SDWanProtos.RoutePacket routePacket) {
        getConnection(routePacket.getSrcVIP(), routePacket.getDstVIP())
                .thenAccept(connection -> {
                    connection.send(routePacket);
                });
    }
}

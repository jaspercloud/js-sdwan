package io.jaspercloud.sdwan.node.support.connection;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.detection.AddressType;
import io.jaspercloud.sdwan.node.support.node.MappingManager;
import io.jaspercloud.sdwan.node.support.node.RelayClient;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.node.support.tunnel.DataTunnel;
import io.jaspercloud.sdwan.node.support.tunnel.P2pManager;
import io.jaspercloud.sdwan.node.support.tunnel.TunnelDataHandler;
import io.jaspercloud.sdwan.stun.AddressAttr;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.jaspercloud.sdwan.stun.StunClient;
import lombok.extern.slf4j.Slf4j;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ConnectionManager implements InitializingBean {

    private Map<String, CompletableFuture<PeerConnection>> connectionMap = new ConcurrentHashMap<>();
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

    public ConnectionManager(SDWanNodeProperties properties, SDWanNode sdWanNode, StunClient stunClient, RelayClient relayClient, MappingManager mappingManager, P2pManager p2pManager) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.relayClient = relayClient;
        this.mappingManager = mappingManager;
        this.p2pManager = p2pManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        p2pManager.addDataHandler(new TunnelDataHandler() {
            @Override
            public void onData(DataTunnel dataTunnel, SDWanProtos.RoutePacket routePacket) {
                try {
                    CompletableFuture<PeerConnection> future = connectionMap.computeIfAbsent(routePacket.getSrcVIP(), key -> {
                        PeerConnection connection = PeerConnection.create(dataTunnel);
                        dataTunnel.addCloseListener(new Consumer<DataTunnel>() {
                            @Override
                            public void accept(DataTunnel dataTunnel) {
                                connection.close();
                            }
                        });
                        connection.addCloseListener(new Consumer<PeerConnection>() {
                            @Override
                            public void accept(PeerConnection peerConnection) {
                                connectionMap.remove(routePacket.getSrcVIP());
                            }
                        });
                        return CompletableFuture.completedFuture(connection);
                    });
                    future.whenComplete((connection, throwable) -> {
                        if (null != throwable) {
                            return;
                        }
                        SDWanProtos.IpPacket ipPacket = connection.receive(routePacket);
                        for (ConnectionDataHandler handler : connectionDataHandlerList) {
                            handler.onData(connection, ipPacket);
                        }
                    });
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    public CompletableFuture<PeerConnection> getConnection(String srcVIP, String dstVIP) {
        CompletableFuture<PeerConnection> result = connectionMap.computeIfAbsent(dstVIP, key -> {
            return sdWanNode.queryNodeInfo(dstVIP)
                    .thenApply(nodeInfo -> {
                        if (SDWanProtos.MessageCode.Success_VALUE != nodeInfo.getCode()) {
                            throw new ProcessException("query nodeInfo error");
                        }
                        return sendBind(nodeInfo, srcVIP, dstVIP);
                    })
                    .thenCompose(f -> f);
        });
        result.whenComplete((connection, error) -> {
            if (null == error) {
                return;
            }
            connectionMap.remove(dstVIP);
        });
        return result;
    }

    private CompletableFuture<PeerConnection> sendBind(SDWanProtos.NodeInfoResp nodeInfo, String srcVIP, String dstVIP) {
        //address
        InetSocketAddress sdWanNodeLocalAddress = (InetSocketAddress) sdWanNode.getChannel().localAddress();
        InetSocketAddress stunClientLocalAddress = (InetSocketAddress) stunClient.getChannel().localAddress();
        String host = UriComponentsBuilder.newInstance()
                .scheme(AddressType.HOST)
                .host(sdWanNodeLocalAddress.getHostString())
                .port(stunClientLocalAddress.getPort())
                .build().toString();
        MappingAddress mappingAddress = mappingManager.getMappingAddress();
        String srflx = UriComponentsBuilder.newInstance()
                .scheme(AddressType.SRFLX)
                .host(mappingAddress.getMappingAddress().getHostString())
                .port(mappingAddress.getMappingAddress().getPort())
                .queryParam("mappingType", mappingAddress.getMappingType().name())
                .build().toString();
        String relay = UriComponentsBuilder.newInstance()
                .scheme(AddressType.RELAY)
                .host(properties.getRelay().getAddress().getHostString())
                .port(properties.getRelay().getAddress().getPort())
                .queryParam("token", relayClient.getRelayToken())
                .build().toString();
        //try target bind
        Map<String, UriComponents> uriComponentsMap = nodeInfo.getAddressListList().stream()
                .map(uri -> UriComponentsBuilder.fromUriString(uri).build())
                .collect(Collectors.toMap(e -> e.getScheme(), e -> e));
        UriComponents components = uriComponentsMap.get("srflx");
        return stunClient.sendBind(new InetSocketAddress(components.getHost(), components.getPort()))
                .handle((bindResp, bindError) -> {
                    if (null != bindError) {
                        return Arrays.asList(host, srflx, relay);
                    }
                    AddressAttr mappedAddressAttr = bindResp.content().getAttr(AttrType.MappedAddress);
                    InetSocketAddress punchAddress = mappedAddressAttr.getAddress();
                    String prflx = UriComponentsBuilder.newInstance()
                            .scheme(AddressType.PRFLX)
                            .host(punchAddress.getHostString())
                            .port(punchAddress.getPort())
                            .build().toString();
                    return Arrays.asList(host, srflx, prflx, relay);
                })
                .thenApply(addressList -> {
                    return createConnection(srcVIP, dstVIP, addressList);
                })
                .thenCompose(f -> f);
    }

    private CompletableFuture<PeerConnection> createConnection(String srcVIP, String dstVIP, List<String> addressList) {
        return PeerConnection.create(p2pManager, srcVIP, dstVIP, addressList)
                .thenApply(connection -> {
                    connection.addCloseListener(new Consumer<PeerConnection>() {
                        @Override
                        public void accept(PeerConnection peerConnection) {
                            connectionMap.remove(dstVIP);
                        }
                    });
                    return connection;
                });
    }

    public void send(SDWanProtos.RoutePacket routePacket) {
        getConnection(routePacket.getSrcVIP(), routePacket.getDstVIP())
                .thenAccept(connection -> {
                    connection.send(routePacket);
                });
    }
}

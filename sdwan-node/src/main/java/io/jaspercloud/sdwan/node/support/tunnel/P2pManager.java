package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.CompletableFutures;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.detection.AddressType;
import io.jaspercloud.sdwan.node.support.detection.DetectionInfo;
import io.jaspercloud.sdwan.node.support.detection.P2pDetection;
import io.jaspercloud.sdwan.node.support.node.RelayClient;
import io.jaspercloud.sdwan.node.support.node.SDWanDataHandler;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.BytesAttr;
import io.jaspercloud.sdwan.stun.MessageType;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.stun.StunDataHandler;
import io.jaspercloud.sdwan.stun.StunMessage;
import io.jaspercloud.sdwan.stun.StunPacket;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class P2pManager implements InitializingBean {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private RelayClient relayClient;
    private Map<String, DataTunnel> tunnelMap = new ConcurrentHashMap<>();
    private Map<String, P2pDetection> detectionMap = new HashMap<>();
    private List<TunnelDataHandler> tunnelDataHandlerList = new ArrayList<>();

    public void addDataHandler(TunnelDataHandler handler) {
        tunnelDataHandlerList.add(handler);
    }

    public void addP2pDetection(P2pDetection detection) {
        detectionMap.put(detection.type(), detection);
    }

    public P2pManager(SDWanNodeProperties properties,
                      SDWanNode sdWanNode,
                      StunClient stunClient,
                      RelayClient relayClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.relayClient = relayClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        stunClient.addDataHandler(new StunDataHandler() {
            @Override
            public void onData(ChannelHandlerContext ctx, StunPacket packet) {
                try {
                    StunMessage request = packet.content();
                    if (MessageType.Transfer.equals(request.getMessageType())) {
                        BytesAttr dataAttr = request.getAttr(AttrType.Data);
                        byte[] data = dataAttr.getData();
                        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.parseFrom(data);
                        SDWanProtos.RoutePacket routePacket = p2pPacket.getPayload();
                        DataTunnel dataTunnel = tunnelMap.get(p2pPacket.getSrcAddress());
                        if (null == dataTunnel) {
                            return;
                        }
                        for (TunnelDataHandler handler : tunnelDataHandlerList) {
                            handler.onData(dataTunnel, routePacket);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        sdWanNode.addDataHandler(new SDWanDataHandler() {
            @Override
            public void onData(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
                try {
                    switch (msg.getType().getNumber()) {
                        case SDWanProtos.MsgTypeCode.P2pOfferType_VALUE: {
                            processP2pOffer(ctx, msg);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        Thread tunnelHeartThread = new Thread(() -> {
            while (true) {
                for (Map.Entry<String, DataTunnel> entry : tunnelMap.entrySet()) {
                    String uri = entry.getKey();
                    DataTunnel dataTunnel = entry.getValue();
                    dataTunnel.check()
                            .whenComplete((result, throwable) -> {
                                if (null == throwable) {
                                    return;
                                }
                                tunnelMap.remove(uri);
                                dataTunnel.close();
                                log.error("p2pHeartTimout: {}", uri);
                            });
                }
                try {
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "p2p-tunnel-heart");
        tunnelHeartThread.setDaemon(true);
        tunnelHeartThread.start();
    }

    private void processP2pOffer(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
        SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(msg.getData());
        List<UriComponents> addressList = p2pOffer.getAddressListList().stream()
                .map(uri -> UriComponentsBuilder.fromUriString(uri).build())
                .collect(Collectors.toList());
        List<CompletableFuture<DetectionInfo>> futureList = addressList.stream()
                .map(e -> {
                    P2pDetection detection = detectionMap.get(e.getScheme());
                    return detection.detection(e.toString());
                }).collect(Collectors.toList());
        CompletableFutures.allOf(futureList)
                .whenComplete((list, error) -> {
                    if (list.isEmpty()) {
                        SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                                .setCode(SDWanProtos.MessageCode.NotFound_VALUE)
                                .build();
                        SDWanProtos.Message resp = msg.toBuilder()
                                .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                                .setData(p2pAnswer.toByteString())
                                .build();
                        ctx.channel().writeAndFlush(resp);
                        return;
                    }
                    DetectionInfo info = list.get(0);
                    UriComponents components = UriComponentsBuilder.fromUriString(info.getDstAddress()).build();
                    if (AddressType.RELAY.equals(components.getScheme())) {
                        InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                        DataTunnel dataTunnel = new RelayDataTunnel(stunClient, relayClient, info, address, components.getQueryParams().getFirst("token"));
                        tunnelMap.computeIfAbsent(info.getDstAddress(), key -> dataTunnel);
                    } else {
                        InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                        DataTunnel dataTunnel = new P2pDataTunnel(stunClient, info, address);
                        tunnelMap.computeIfAbsent(info.getDstAddress(), key -> dataTunnel);
                    }
                    SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                            .setCode(SDWanProtos.MessageCode.Success_VALUE)
                            .setSrcVIP(p2pOffer.getDstVIP())
                            .setDstVIP(p2pOffer.getSrcVIP())
                            .setSrcAddress(info.getSrcAddress())
                            .setDstAddress(info.getDstAddress())
                            .build();
                    SDWanProtos.Message resp = msg.toBuilder()
                            .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                            .setData(p2pAnswer.toByteString())
                            .build();
                    ctx.channel().writeAndFlush(resp);
                });
    }

    public CompletableFuture<DataTunnel> create(String srcVIP, String dstVIP, List<String> addressList) {
        return sdWanNode.offer(srcVIP, dstVIP, addressList)
                .thenApply(result -> {
                    int code = result.getCode();
                    if (SDWanProtos.MessageCode.Success_VALUE != code) {
                        throw new ProcessException("offer error: " + code);
                    }
                    DetectionInfo info = new DetectionInfo(result.getDstAddress(), result.getSrcAddress());
                    UriComponents components = UriComponentsBuilder.fromUriString(info.getDstAddress()).build();
                    DataTunnel dataTunnel;
                    if (AddressType.RELAY.equals(components.getScheme())) {
                        InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                        dataTunnel = new RelayDataTunnel(stunClient, relayClient, info, address, components.getQueryParams().getFirst("token"));
                        tunnelMap.computeIfAbsent(info.getDstAddress(), key -> dataTunnel);
                    } else {
                        InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                        dataTunnel = new P2pDataTunnel(stunClient, info, address);
                        tunnelMap.computeIfAbsent(info.getDstAddress(), key -> dataTunnel);
                    }
                    return dataTunnel;
                });
    }
}

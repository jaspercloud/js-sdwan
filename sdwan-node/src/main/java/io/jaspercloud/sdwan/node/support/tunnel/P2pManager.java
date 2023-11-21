package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.CompletableFuturePlus;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.SDWanDataHandler;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.detection.AddressType;
import io.jaspercloud.sdwan.node.support.detection.P2pDetection;
import io.jaspercloud.sdwan.stun.StunClient;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
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
    private Map<InetSocketAddress, DataTunnel> tunnelMap = new ConcurrentHashMap<>();
    private Map<String, P2pDetection> detectionMap = new HashMap<>();

    public void addP2pDetection(P2pDetection detection) {
        detectionMap.put(detection.type(), detection);
    }

    public P2pManager(SDWanNodeProperties properties,
                      SDWanNode sdWanNode,
                      StunClient stunClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sdWanNode.addDataHandler(new SDWanDataHandler<SDWanProtos.Message>() {
            @Override
            public void onData(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                switch (msg.getType().getNumber()) {
                    case SDWanProtos.MsgTypeCode.P2pOfferType_VALUE: {
                        SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(msg.getData());
                        List<UriComponents> addressList = p2pOffer.getAddressListList().stream()
                                .map(uri -> UriComponentsBuilder.fromUriString(uri).build())
                                .collect(Collectors.toList());
                        Iterator<UriComponents> iterator = addressList.iterator();
                        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                            throw new ProcessException("init");
                        });
                        while (iterator.hasNext()) {
                            UriComponents next = iterator.next();
                            P2pDetection detection = detectionMap.get(next.getScheme());
                            future = CompletableFuturePlus.onException(future, () -> {
                                return detection.detection(next.toUriString());
                            });
                        }
                        future.whenComplete((uri, error) -> {
                            if (null != error) {
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
                            UriComponents components = UriComponentsBuilder.fromUriString(uri).build();
                            if (AddressType.RELAY.equals(components.getScheme())) {
                                InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                                DataTunnel dataTunnel = new RelayDataTunnel(stunClient, address, components.getQueryParams().getFirst("token"));
                                tunnelMap.put(address, dataTunnel);
                            } else {
                                InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                                DataTunnel dataTunnel = new P2pDataTunnel(stunClient, address);
                                tunnelMap.put(address, dataTunnel);
                            }
                            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                                    .setCode(SDWanProtos.MessageCode.Success_VALUE)
                                    .setSrcVIP(p2pOffer.getDstVIP())
                                    .setDstVIP(p2pOffer.getSrcVIP())
                                    .setAddress(uri)
                                    .build();
                            SDWanProtos.Message resp = msg.toBuilder()
                                    .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                                    .setData(p2pAnswer.toByteString())
                                    .build();
                            ctx.channel().writeAndFlush(resp);
                        });
                        break;
                    }
                }
            }
        });
        Thread tunnelHeartThread = new Thread(() -> {
            while (true) {
                for (Map.Entry<InetSocketAddress, DataTunnel> entry : tunnelMap.entrySet()) {
                    InetSocketAddress addr = entry.getKey();
                    DataTunnel dataTunnel = entry.getValue();
                    dataTunnel.check()
                            .whenComplete((result, throwable) -> {
                                if (null == throwable) {
                                    return;
                                }
                                tunnelMap.remove(addr);
                                log.error("punchingHeartTimout: {}", addr);
                            });
                }
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "p2p-tunnel-heart");
        tunnelHeartThread.setDaemon(true);
        tunnelHeartThread.start();
    }

    public CompletableFuture<DataTunnel> create(String srcVIP, String dstVIP, List<String> list) {
        return sdWanNode.offer(srcVIP, dstVIP, list)
                .thenApply(result -> {
                    int code = result.getCode();
                    if (SDWanProtos.MessageCode.Success_VALUE != code) {
                        throw new ProcessException("offer error: " + code);
                    }
                    UriComponents components = UriComponentsBuilder.fromUriString(result.getAddress()).build();
                    DataTunnel dataTunnel;
                    if (AddressType.RELAY.equals(components.getScheme())) {
                        InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                        dataTunnel = new RelayDataTunnel(stunClient, address, components.getQueryParams().getFirst("token"));
                        tunnelMap.put(address, dataTunnel);
                    } else {
                        InetSocketAddress address = new InetSocketAddress(components.getHost(), components.getPort());
                        dataTunnel = new P2pDataTunnel(stunClient, address);
                        tunnelMap.put(address, dataTunnel);
                    }
                    return dataTunnel;
                });
    }
}

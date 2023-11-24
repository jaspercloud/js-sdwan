package io.jaspercloud.sdwan.node.support.detection;

import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.AddressAttr;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.StunClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class SrflxP2pDetection implements P2pDetection {

    private SDWanNodeProperties properties;
    private StunClient stunClient;

    public SrflxP2pDetection(SDWanNodeProperties properties, StunClient stunClient) {
        this.properties = properties;
        this.stunClient = stunClient;
    }

    @Override
    public String type() {
        return AddressType.SRFLX;
    }

    @Override
    public CompletableFuture<DetectionInfo> detection(String uri) {
        Long timeout = properties.getStun().getHeartTimeout();
        UriComponents components = UriComponentsBuilder.fromUriString(uri).build();
        return stunClient.sendBind(new InetSocketAddress(components.getHost(), components.getPort()), timeout)
                .thenApply(resp -> {
                    AddressAttr addressAttr = resp.content().getAttr(AttrType.MappedAddress);
                    InetSocketAddress address = addressAttr.getAddress();
                    String selfUri = UriComponentsBuilder.newInstance()
                            .scheme(AddressType.PRFLX)
                            .host(address.getHostString())
                            .port(address.getPort())
                            .build().toString();
                    return new DetectionInfo(selfUri, uri);
                });
    }
}

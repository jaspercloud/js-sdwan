package io.jaspercloud.sdwan.node.support.detection;

import io.jaspercloud.sdwan.stun.AddressAttr;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.StunClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class HostP2pDetection implements P2pDetection {

    private StunClient stunClient;

    public HostP2pDetection(StunClient stunClient) {
        this.stunClient = stunClient;
    }

    @Override
    public String type() {
        return AddressType.HOST;
    }

    @Override
    public CompletableFuture<DetectionInfo> detection(String uri) {
        UriComponents components = UriComponentsBuilder.fromUriString(uri).build();
        return stunClient.sendBind(new InetSocketAddress(components.getHost(), components.getPort()))
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

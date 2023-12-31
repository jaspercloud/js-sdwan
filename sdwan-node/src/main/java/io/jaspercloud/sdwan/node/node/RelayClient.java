package io.jaspercloud.sdwan.node.node;

import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private String localRelayToken;

    public InetSocketAddress getRelayAddress() {
        return properties.getRelay().getAddress();
    }

    public String getRelayToken() {
        return localRelayToken;
    }

    public RelayClient(SDWanNodeProperties properties, SDWanNode sdWanNode, StunClient stunClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        InetSocketAddress address = (InetSocketAddress) sdWanNode.getChannel().localAddress();
        localRelayToken = address.getHostString() + "-" + UUID.randomUUID().toString().replaceAll("\\-", "");
        new Thread(() -> {
            while (true) {
                try {
                    bind();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof TimeoutException) {
                        log.error("relayClient heart timeout");
                    } else {
                        log.error(e.getMessage(), e);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "relay-heart").start();
    }

    private void bind() throws Exception {
        SDWanNodeProperties.Relay relay = properties.getRelay();
        StunMessage req = new StunMessage(MessageType.BindRelayRequest);
        req.setAttr(AttrType.RelayToken, new StringAttr(localRelayToken));
        stunClient.invokeAsync(new StunPacket(req, relay.getAddress()), relay.getHeartTimeout()).get();
    }
}

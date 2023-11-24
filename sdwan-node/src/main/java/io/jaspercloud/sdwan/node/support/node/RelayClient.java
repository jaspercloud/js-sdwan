package io.jaspercloud.sdwan.node.support.node;

import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.UUID;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private String localRelayToken;

    public InetSocketAddress getRelayAddress() {
        return properties.getRelayServer();
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
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "relay-heart").start();
    }

    private void bind() throws Exception {
        StunMessage req = new StunMessage(MessageType.BindRelayRequest);
        req.setAttr(AttrType.RelayToken, new StringAttr(localRelayToken));
        stunClient.invokeAsync(new StunPacket(req, properties.getRelayServer())).get();
    }
}

package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.stun.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.UUID;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private StunClient stunClient;
    private String relayToken = UUID.randomUUID().toString();

    public String getRelayToken() {
        return relayToken;
    }

    public RelayClient(SDWanNodeProperties properties, StunClient stunClient) {
        this.properties = properties;
        this.stunClient = stunClient;
    }

    public void addStunDataHandler(StunDataHandler<StunMessage> handler) {
        stunClient.addStunDataHandler(handler);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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
        req.setAttr(AttrType.RelayToken, new StringAttr(relayToken));
        stunClient.invokeSync(new StunPacket(req, properties.getRelayServer()));
    }

    public void send(StunMessage message) {
        stunClient.send(properties.getRelayServer(), message);
    }
}

package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.stun.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private StunClient stunClient;
    private String localVIP;

    public void setLocalVIP(String localVIP) {
        this.localVIP = localVIP;
    }

    public RelayClient(SDWanNodeProperties properties, StunClient stunClient) {
        this.properties = properties;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(() -> {
            while (true) {
                if (StringUtils.isNotEmpty(localVIP)) {
                    try {
                        stunClient.sendBindRelay(properties.getRelayServer(), localVIP, 3000).get();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "relay-client-heart");
        thread.setDaemon(true);
        thread.start();
    }

    public StunPacket createRelayPacket(String localVIP, String dstVIP, ByteBuf byteBuf) {
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.SrcVIP, new StringAttr(localVIP));
        message.getAttrs().put(AttrType.DstVIP, new StringAttr(dstVIP));
        message.getAttrs().put(AttrType.Data, new ByteBufAttr(byteBuf));
        StunPacket packet = new StunPacket(message, properties.getRelayServer());
        return packet;
    }
}

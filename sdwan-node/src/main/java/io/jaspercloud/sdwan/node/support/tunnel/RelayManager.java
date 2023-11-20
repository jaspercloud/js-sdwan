package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RelayManager implements InitializingBean {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private String relayToken = UUID.randomUUID().toString();
    private Map<String, DataTunnel> tunnelMap = new ConcurrentHashMap<>();

    public RelayManager(SDWanNodeProperties properties, SDWanNode sdWanNode, StunClient stunClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    StunMessage req = new StunMessage(MessageType.BindRelayRequest);
                    req.setAttr(AttrType.RelayToken, new StringAttr(relayToken));
                    stunClient.invokeSync(new StunPacket(req, properties.getRelayServer())).content();
                } catch (TimeoutException e) {
                    Iterator<Map.Entry<String, DataTunnel>> iterator = tunnelMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, DataTunnel> next = iterator.next();
                        DataTunnel dataTunnel = next.getValue();
                        dataTunnel.close();
                        iterator.remove();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
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
        stunClient.addStunDataHandler(new StunDataHandler<StunMessage>() {
            @Override
            protected void onData(ChannelHandlerContext ctx, StunMessage msg) {
                if (MessageType.Transfer.equals(msg.getMessageType())) {
                    try {
                        BytesAttr bytesAttr = (BytesAttr) msg.getAttrs().get(AttrType.Data);
                        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.parseFrom(bytesAttr.getData());
                        System.out.println();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    public String getToken() {
        return relayToken;
    }

    public CompletableFuture<Boolean> check(String relayToken) {
        return sdWanNode.checkRelayToken(relayToken);
    }

    public void addTunnel(String relayToken, DataTunnel dataTunnel) {
        tunnelMap.put(relayToken, dataTunnel);
    }
}

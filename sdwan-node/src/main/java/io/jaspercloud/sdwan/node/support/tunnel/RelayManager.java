package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.RelayClient;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RelayManager implements InitializingBean {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private RelayClient relayClient;
    private Map<String, DataTunnel> tunnelMap = new ConcurrentHashMap<>();

    public RelayManager(SDWanNodeProperties properties, SDWanNode sdWanNode, RelayClient relayClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.relayClient = relayClient;
    }

    public String getToken() {
        return relayClient.getRelayToken();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        relayClient.addStunDataHandler(new StunDataHandler<StunMessage>() {
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

    public CompletableFuture<Boolean> check(String relayToken) {
        return sdWanNode.checkRelayToken(relayToken);
    }

    public void addTunnel(String relayToken, DataTunnel dataTunnel) {
        tunnelMap.put(relayToken, dataTunnel);
    }
}

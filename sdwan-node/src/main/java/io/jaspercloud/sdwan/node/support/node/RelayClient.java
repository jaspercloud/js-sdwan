package io.jaspercloud.sdwan.node.support.node;

import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RelayClient implements InitializingBean {

    private SDWanNodeProperties properties;
    private StunClient stunClient;
    private String localRelayToken = UUID.randomUUID().toString();

    public InetSocketAddress getRelayAddress() {
        return properties.getRelayServer();
    }

    public String getRelayToken() {
        return localRelayToken;
    }

    public RelayClient(SDWanNodeProperties properties, StunClient stunClient) {
        this.properties = properties;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        stunClient.addDataHandler(new StunDataHandler<StunPacket>() {
            @Override
            protected void onData(ChannelHandlerContext ctx, StunPacket packet) {
                StunMessage request = packet.content();
                InetSocketAddress sender = packet.sender();
                if (MessageType.HeartRequest.equals(request.getMessageType())) {
                    StringAttr srcToken = request.getAttr(AttrType.SrcRelayToken);
                    StringAttr dstToken = request.getAttr(AttrType.DstRelayToken);
                    String token = dstToken.getData();
                    if (!StringUtils.equals(token, localRelayToken)) {
                        return;
                    }
                    request.getAttrs().put(AttrType.SrcRelayToken, dstToken);
                    request.getAttrs().put(AttrType.DstRelayToken, srcToken);
                    StunMessage response = new StunMessage(MessageType.HeartResponse, request.getTranId());
                    ctx.writeAndFlush(new StunPacket(response, sender));
                }
            }
        });
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
        req.setAttr(AttrType.DstRelayToken, new StringAttr(localRelayToken));
        stunClient.invokeAsync(new StunPacket(req, properties.getRelayServer())).get();
    }

    public CompletableFuture<StunPacket> sendHeart(InetSocketAddress relayAddr, String dstRelayToken) {
        StunMessage req = new StunMessage(MessageType.HeartRequest);
        req.setAttr(AttrType.SrcRelayToken, new StringAttr(localRelayToken));
        req.setAttr(AttrType.DstRelayToken, new StringAttr(dstRelayToken));
        return stunClient.invokeAsync(new StunPacket(req, relayAddr));
    }
}

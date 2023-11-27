package io.jaspercloud.sdwan.domain.relay.service.impl;

import io.jaspercloud.sdwan.domain.relay.service.RelayNodeManager;
import io.jaspercloud.sdwan.domain.relay.service.RelayService;
import io.jaspercloud.sdwan.domain.relay.vo.RelayNode;
import io.jaspercloud.sdwan.stun.*;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

@Slf4j
@Service
public class RelayServiceImpl implements RelayService {

    @Resource
    private RelayNodeManager nodeManager;

    @Override
    public void processBindRelay(Channel channel, StunPacket packet) {
        InetSocketAddress sender = packet.sender();
        StunMessage request = packet.content();
        //parse
        StringAttr relayTokenAttr = (StringAttr) request.getAttrs().get(AttrType.RelayToken);
        String relayToken = relayTokenAttr.getData();
        nodeManager.addNode(relayToken, sender);
        //resp
        StunMessage responseMessage = new StunMessage(MessageType.BindRelayResponse, request.getTranId());
        StunPacket response = new StunPacket(responseMessage, sender);
        channel.writeAndFlush(response);
    }

    @Override
    public void processTransfer(Channel channel, StunPacket packet) {
        InetSocketAddress sender = packet.sender();
        StunMessage request = packet.content();
        //parse
        StringAttr relayTokenAttr = (StringAttr) request.getAttrs().get(AttrType.RelayToken);
        String relayToken = relayTokenAttr.getData();
        RelayNode node = nodeManager.getNode(relayToken);
        if (null == node) {
            log.warn("not found node: relayToken={}", relayToken);
            return;
        }
        //resp
        BytesAttr dataAttr = (BytesAttr) request.getAttrs().get(AttrType.Data);
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.Data, dataAttr);
        StunPacket response = new StunPacket(message, node.getTargetAddress());
        channel.writeAndFlush(response);
    }
}

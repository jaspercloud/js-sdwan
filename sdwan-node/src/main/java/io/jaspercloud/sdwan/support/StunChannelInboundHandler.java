package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.MessageType;
import io.jaspercloud.sdwan.StunMessage;
import io.jaspercloud.sdwan.StunPacket;
import io.netty.channel.SimpleChannelInboundHandler;

public abstract class StunChannelInboundHandler extends SimpleChannelInboundHandler<StunPacket> {

    private MessageType messageType;

    public StunChannelInboundHandler(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        boolean accept = super.acceptInboundMessage(msg);
        if (accept) {
            StunPacket packet = (StunPacket) msg;
            StunMessage request = packet.content();
            accept = messageType.equals(request.getMessageType());
        }
        return accept;
    }
}

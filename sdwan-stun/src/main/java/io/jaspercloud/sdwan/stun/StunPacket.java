package io.jaspercloud.sdwan.stun;

import io.netty.channel.DefaultAddressedEnvelope;

import java.net.InetSocketAddress;

public class StunPacket extends DefaultAddressedEnvelope<StunMessage, InetSocketAddress> {

    public StunPacket(StunMessage message, InetSocketAddress recipient) {
        super(message, recipient);
    }

    public StunPacket(StunMessage message, InetSocketAddress recipient, InetSocketAddress sender) {
        super(message, recipient, sender);
    }

    @Override
    public StunPacket retain() {
        return retain(1);
    }

    @Override
    public StunPacket retain(int increment) {
        StunMessage message = content();
        message.retain(increment);
        return this;
    }
}
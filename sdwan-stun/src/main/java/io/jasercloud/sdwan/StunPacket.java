package io.jasercloud.sdwan;

import io.netty.channel.DefaultAddressedEnvelope;

import java.net.InetSocketAddress;

public class StunPacket extends DefaultAddressedEnvelope<StunMessage, InetSocketAddress> {

    public StunPacket(StunMessage message, InetSocketAddress recipient) {
        super(message, recipient);
    }
}
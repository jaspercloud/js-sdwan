package io.jasercloud.sdwan.support.transporter;

import io.jasercloud.sdwan.tun.IpPacket;
import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

public interface Transporter {

    void writePacket(InetSocketAddress address, ByteBuf byteBuf);

    void setReceiveHandler(ReceiveHandler handler);

    interface ReceiveHandler {

        void onPacket(IpPacket ipPacket);
    }
}

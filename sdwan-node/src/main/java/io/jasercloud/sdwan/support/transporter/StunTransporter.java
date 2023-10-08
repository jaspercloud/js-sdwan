package io.jasercloud.sdwan.support.transporter;

import io.jasercloud.sdwan.*;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class StunTransporter extends StunClient implements Transporter {

    private ReceiveHandler handler;

    public StunTransporter(InetSocketAddress local, InetSocketAddress stunServer) {
        super(local, stunServer);
    }

    @Override
    protected void processForward(StunPacket packet) {
        DataAttr dataAttr = (DataAttr) packet.content().getAttrs().get(AttrType.Data);
        ByteBuf byteBuf = dataAttr.getByteBuf();
        Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
        handler.onPacket(ipv4Packet);
    }

    @Override
    public void writePacket(InetSocketAddress address, ByteBuf byteBuf) {
        StunMessage message = new StunMessage(MessageType.Forward);
        message.getAttrs().put(AttrType.Data, new DataAttr(byteBuf));
        StunPacket request = new StunPacket(message, address);
        getChannel().writeAndFlush(request);
    }

    @Override
    public void setReceiveHandler(ReceiveHandler handler) {
        this.handler = handler;
    }
}

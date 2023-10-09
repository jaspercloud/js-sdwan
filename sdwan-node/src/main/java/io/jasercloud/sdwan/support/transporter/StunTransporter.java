package io.jasercloud.sdwan.support.transporter;

import io.jasercloud.sdwan.*;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.TunChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class StunTransporter implements Transporter {

    private StunClient stunClient;

    public StunTransporter(StunClient stunClient) {
        this.stunClient = stunClient;
    }

    @Override
    public void bind(TunChannel tunChannel) {
        stunClient.getChannel().pipeline().addLast(new SimpleChannelInboundHandler<StunPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                InetSocketAddress address = packet.sender();
                DataAttr dataAttr = (DataAttr) packet.content().getAttrs().get(AttrType.Data);
                ByteBuf byteBuf = dataAttr.getByteBuf();
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                log.info("input: {} -> {} -> {}",
                        address.getHostString(), ipv4Packet.getSrcIP(), ipv4Packet.getDstIP());
                tunChannel.writeAndFlush(byteBuf.retain());
            }
        });
        tunChannel.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
                InetSocketAddress address = packet.recipient();
                ByteBuf byteBuf = packet.content();
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                log.info("output: {} -> {} -> {}",
                        ipv4Packet.getSrcIP(), ipv4Packet.getDstIP(), address.getHostString());
                StunMessage message = new StunMessage(MessageType.Transfer);
                message.getAttrs().put(AttrType.Data, new DataAttr(byteBuf.retain()));
                StunPacket request = new StunPacket(message, address);
                stunClient.getChannel().writeAndFlush(request);
            }
        });
    }
}

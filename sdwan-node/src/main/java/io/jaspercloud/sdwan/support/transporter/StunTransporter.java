package io.jaspercloud.sdwan.support.transporter;

import io.jaspercloud.sdwan.AttrType;
import io.jaspercloud.sdwan.ByteBufAttr;
import io.jaspercloud.sdwan.MessageType;
import io.jaspercloud.sdwan.StunClient;
import io.jaspercloud.sdwan.StunMessage;
import io.jaspercloud.sdwan.StunPacket;
import io.jaspercloud.sdwan.support.StunChannelInboundHandler;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class StunTransporter implements Transporter {

    private StunClient stunClient;
    private List<Filter> filterList;

    public StunTransporter(StunClient stunClient, List<Filter> filterList) {
        this.stunClient = stunClient;
        this.filterList = filterList;
    }

    @Override
    public void bind(TunChannel tunChannel) {
        stunClient.getChannel().pipeline().addLast(new StunChannelInboundHandler(MessageType.Transfer) {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket stunPacket) throws Exception {
                InetSocketAddress address = stunPacket.sender();
                ByteBufAttr byteBufAttr = (ByteBufAttr) stunPacket.content().getAttrs().get(AttrType.Data);
                ByteBuf byteBuf = byteBufAttr.getByteBuf();
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                log.debug("input: {} -> {} -> {}",
                        address.getHostString(), ipv4Packet.getSrcIP(), ipv4Packet.getDstIP());
                for (Filter filter : filterList) {
                    byteBuf = filter.decode(address, byteBuf);
                }
                tunChannel.writeAndFlush(byteBuf.retain());
            }
        });
        tunChannel.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
                InetSocketAddress address = packet.recipient();
                ByteBuf byteBuf = packet.content();
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                log.debug("output: {} -> {} -> {}",
                        ipv4Packet.getSrcIP(), ipv4Packet.getDstIP(), address.getHostString());
                for (Filter filter : filterList) {
                    byteBuf = filter.encode(address, byteBuf);
                }
                StunMessage message = new StunMessage(MessageType.Transfer);
                message.getAttrs().put(AttrType.Data, new ByteBufAttr(byteBuf));
                StunPacket request = new StunPacket(message, address);
                stunClient.getChannel().writeAndFlush(request.retain());
            }
        });
    }
}

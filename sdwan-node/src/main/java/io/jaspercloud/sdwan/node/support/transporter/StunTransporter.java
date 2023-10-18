package io.jaspercloud.sdwan.node.support.transporter;

import io.jaspercloud.sdwan.node.support.StunChannelInboundHandler;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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
        stunClient.getChannel().pipeline().addLast("Transporter:readStun", new StunChannelInboundHandler(MessageType.Transfer) {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket stunPacket) throws Exception {
                InetSocketAddress address = stunPacket.sender();
                ByteBufAttr byteBufAttr = (ByteBufAttr) stunPacket.content().getAttrs().get(AttrType.Data);
                ByteBuf byteBuf = byteBufAttr.getByteBuf().retain();
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                log.debug("input: {} -> {} -> {}",
                        address.getHostString(), ipv4Packet.getSrcIP(), ipv4Packet.getDstIP());
                for (Filter filter : filterList) {
                    byteBuf = filter.decode(address, byteBuf);
                }
                tunChannel.writeAndFlush(byteBuf);
            }
        });
        tunChannel.pipeline().addLast("Transporter:readTun", new SimpleChannelInboundHandler<StunPacket>() {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                InetSocketAddress address = packet.recipient();
                StunMessage stunMessage = packet.content();
                ByteBufAttr dataAttr = (ByteBufAttr) stunMessage.getAttrs().get(AttrType.Data);
                ByteBuf byteBuf = dataAttr.getByteBuf();
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                log.debug("output: {} -> {} -> {}",
                        ipv4Packet.getSrcIP(), ipv4Packet.getDstIP(), address.getHostString());
                for (Filter filter : filterList) {
                    byteBuf = filter.encode(address, byteBuf);
                }
                stunMessage.getAttrs().put(AttrType.Data, new ByteBufAttr(byteBuf));
                stunClient.getChannel().writeAndFlush(packet.retain());
            }
        });
    }
}

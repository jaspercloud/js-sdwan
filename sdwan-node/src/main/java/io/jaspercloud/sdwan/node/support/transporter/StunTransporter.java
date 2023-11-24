//package io.jaspercloud.sdwan.node.support.transporter;
//
//import io.jaspercloud.sdwan.ByteBufUtil;
//import io.jaspercloud.sdwan.stun.*;
//import io.jaspercloud.sdwan.tun.Ipv4Packet;
//import io.jaspercloud.sdwan.tun.TunChannel;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.SimpleChannelInboundHandler;
//import lombok.extern.slf4j.Slf4j;
//
//import java.net.InetSocketAddress;
//import java.util.List;
//
//@Slf4j
//public class StunTransporter implements Transporter {
//
//    private StunClient stunClient;
//    private List<Filter> filterList;
//
//    public StunTransporter(StunClient stunClient, List<Filter> filterList) {
//        this.stunClient = stunClient;
//        this.filterList = filterList;
//    }
//
//    @Override
//    public void bind(TunChannel tunChannel) {
//        stunClient.getChannel().pipeline().addLast("Transporter:readStun", new StunChannelInboundHandler(MessageType.Transfer) {
//
//            @Override
//            protected void channelRead0(ChannelHandlerContext ctx, StunPacket stunPacket) throws Exception {
//                InetSocketAddress address = stunPacket.sender();
//                BytesAttr bytesAttr = (BytesAttr) stunPacket.content().getAttrs().get(AttrType.Data);
//                byte[] data = bytesAttr.getData();
//                for (Filter filter : filterList) {
//                    data = filter.decode(address, data);
//                }
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(ByteBufUtil.toByteBuf(data));
//                try {
//                    log.debug("input: {} -> {} -> {}",
//                            address.getHostString(), ipv4Packet.getSrcIP(), ipv4Packet.getDstIP());
//                } finally {
//                    ipv4Packet.release();
//                }
//                tunChannel.writeAndFlush(ByteBufUtil.toByteBuf(data));
//            }
//        });
//        tunChannel.pipeline().addLast("Transporter:readTun", new SimpleChannelInboundHandler<StunPacket>() {
//
//            @Override
//            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
//                InetSocketAddress address = packet.recipient();
//                StunMessage stunMessage = packet.content();
//                BytesAttr dataAttr = (BytesAttr) stunMessage.getAttrs().get(AttrType.Data);
//                byte[] data = dataAttr.getData();
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(ByteBufUtil.toByteBuf(data));
//                try {
//                    log.debug("output: {} -> {} -> {}",
//                            ipv4Packet.getSrcIP(), ipv4Packet.getDstIP(), address.getHostString());
//                } finally {
//                    ipv4Packet.release();
//                }
//                for (Filter filter : filterList) {
//                    data = filter.encode(address, data);
//                }
//                stunMessage.getAttrs().put(AttrType.Data, new BytesAttr(data));
//                stunClient.getChannel().writeAndFlush(packet);
//            }
//        });
//    }
//}

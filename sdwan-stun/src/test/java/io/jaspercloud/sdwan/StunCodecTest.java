//package io.jaspercloud.sdwan;
//
//import io.jaspercloud.sdwan.stun.AddressAttr;
//import io.jaspercloud.sdwan.stun.AttrType;
//import io.jaspercloud.sdwan.stun.BytesAttr;
//import io.jaspercloud.sdwan.stun.MessageType;
//import io.jaspercloud.sdwan.stun.ProtoFamily;
//import io.jaspercloud.sdwan.stun.StringAttr;
//import io.jaspercloud.sdwan.stun.StunDecoder;
//import io.jaspercloud.sdwan.stun.StunEncoder;
//import io.jaspercloud.sdwan.stun.StunMessage;
//import io.jaspercloud.sdwan.stun.StunPacket;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelPipeline;
//import io.netty.channel.embedded.EmbeddedChannel;
//import io.netty.channel.socket.DatagramPacket;
//
//import java.net.InetSocketAddress;
//
//public class StunCodecTest {
//
//    public static void main(String[] args) {
//        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInitializer<Channel>() {
//            @Override
//            protected void initChannel(Channel channel) throws Exception {
//                ChannelPipeline pipeline = channel.pipeline();
//                pipeline.addLast(new StunEncoder());
//                pipeline.addLast(new StunDecoder());
//            }
//        });
//        StunMessage stunMessage = new StunMessage(MessageType.Transfer);
//        stunMessage.getAttrs().put(AttrType.EncryptKey, new StringAttr("key"));
//        stunMessage.getAttrs().put(AttrType.VIP, new StringAttr("vip"));
//        stunMessage.getAttrs().put(AttrType.Data, new BytesAttr("data".getBytes()));
//        stunMessage.getAttrs().put(AttrType.MappedAddress, new AddressAttr(ProtoFamily.IPv4, "127.0.0.1", 8888));
//        StunPacket stunPacket = new StunPacket(stunMessage, new InetSocketAddress("127.0.0.1", 80));
//        channel.writeAndFlush(stunPacket);
//        channel.writeInbound((DatagramPacket) channel.readOutbound());
//        StunPacket response = channel.readInbound();
//        System.out.println();
//    }
//}

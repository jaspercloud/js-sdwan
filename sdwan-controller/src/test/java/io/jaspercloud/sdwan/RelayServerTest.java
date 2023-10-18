package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.infra.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.infra.support.RelayServer;
import io.jaspercloud.sdwan.stun.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

public class RelayServerTest {

    public static void main(String[] args) throws Exception {
        SDWanRelayProperties properties = new SDWanRelayProperties();
        properties.setPort(888);
        properties.setTimeout(5000L);
        RelayServer relayServer = new RelayServer(properties);
        relayServer.afterPropertiesSet();
        StunClient stunClient1 = new StunClient(new InetSocketAddress("0.0.0.0", 5551));
        stunClient1.afterPropertiesSet();
        stunClient1.getChannel().pipeline().addLast(new SimpleChannelInboundHandler<StunPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket msg) throws Exception {
                System.out.println();
            }
        });
        StunPacket stunPacket1 = stunClient1.sendAllocate(new InetSocketAddress("127.0.0.1", 888), 3000).get();
        StringAttr channelIdAttr = (StringAttr) stunPacket1.content().getAttrs().get(AttrType.ChannelId);
        String channelId = channelIdAttr.getData();
        new Thread(() -> {
            stunClient1.sendAllocateRefresh(new InetSocketAddress("127.0.0.1", 888), channelId);
        }).start();
        StunClient stunClient2 = new StunClient(new InetSocketAddress("0.0.0.0", 5552));
        stunClient2.afterPropertiesSet();
        StunPacket stunPacket2 = stunClient2.sendAllocate(new InetSocketAddress("127.0.0.1", 888), 3000).get();
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.ChannelId, new StringAttr(channelId));
        message.getAttrs().put(AttrType.Data, new ByteBufAttr(Unpooled.wrappedBuffer("test".getBytes())));
        StunPacket stunPacket = new StunPacket(message, new InetSocketAddress("127.0.0.1", 888));
        stunClient2.getChannel().writeAndFlush(stunPacket);
        stunClient2.getChannel().closeFuture().sync();
        System.out.println();
    }
}

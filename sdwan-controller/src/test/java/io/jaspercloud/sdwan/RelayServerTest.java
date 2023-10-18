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
                StunMessage stunMessage = msg.content();
                LongAttr liveTimeAttr = (LongAttr) stunMessage.getAttrs().get(AttrType.LiveTime);
                if (null != liveTimeAttr) {
                    System.out.println("liveTime:" + liveTimeAttr.getData());
                }
                ByteBufAttr byteBufAttr = (ByteBufAttr) stunMessage.getAttrs().get(AttrType.Data);
                if (null != byteBufAttr) {
                    byte[] bytes = ByteBufUtil.toBytes(byteBufAttr.getByteBuf());
                    String text = new String(bytes);
                    System.out.println(text);
                }
            }
        });
        StunPacket stunPacket1 = stunClient1.sendAllocate(new InetSocketAddress("127.0.0.1", 888), 3000).get();
        StringAttr channelIdAttr = (StringAttr) stunPacket1.content().getAttrs().get(AttrType.ChannelId);
        String channelId = channelIdAttr.getData();
        new Thread(() -> {
            while (true) {
                stunClient1.sendAllocateRefresh(new InetSocketAddress("127.0.0.1", 888), channelId, 3000);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        StunClient stunClient2 = new StunClient(new InetSocketAddress("0.0.0.0", 5552));
        stunClient2.afterPropertiesSet();
        stunClient2.sendAllocate(new InetSocketAddress("127.0.0.1", 888), 3000).get();
        int n = 1;
        while (true) {
            StunMessage message = new StunMessage(MessageType.Transfer);
            message.getAttrs().put(AttrType.ChannelId, new StringAttr(channelId));
            message.getAttrs().put(AttrType.Data, new ByteBufAttr(Unpooled.wrappedBuffer(("test" + n++).getBytes())));
            StunPacket stunPacket = new StunPacket(message, new InetSocketAddress("127.0.0.1", 888));
            stunClient2.sendTurnData(stunPacket);
            Thread.sleep(1000);
        }
    }
}

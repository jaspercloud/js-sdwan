package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.stun.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class DataTest {

    public static void main(String[] args) throws Exception {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1001);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 1002);
        StunClient stunClient1 = new StunClient(address1);
        stunClient1.afterPropertiesSet();
        StunClient stunClient2 = new StunClient(address2);
        stunClient2.afterPropertiesSet();
        ByteBuf byteBuf = Unpooled.wrappedBuffer("test".getBytes(StandardCharsets.UTF_8));
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.Data, new ByteBufAttr(byteBuf));
        StunPacket request = new StunPacket(message, address2);
        stunClient1.getChannel().writeAndFlush(request);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

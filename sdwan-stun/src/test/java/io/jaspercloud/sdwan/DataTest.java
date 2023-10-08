package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class DataTest {

    public static void main(String[] args) throws Exception {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1001);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 1002);
        StunClient stunClient1 = new StunClient(address1, null);
        stunClient1.afterPropertiesSet();
        StunClient stunClient2 = new StunClient(address2, null);
        stunClient2.afterPropertiesSet();

        ByteBuf byteBuf = Unpooled.wrappedBuffer("test".getBytes(StandardCharsets.UTF_8));
        StunMessage message = new StunMessage(MessageType.Forward);
        message.getAttrs().put(AttrType.Data, new DataAttr(byteBuf));
        StunPacket request = new StunPacket(message, address2);
        stunClient1.getChannel().writeAndFlush(request);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

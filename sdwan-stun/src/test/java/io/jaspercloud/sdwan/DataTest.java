package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.BytesAttr;
import io.jaspercloud.sdwan.stun.MessageType;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.stun.StunMessage;
import io.jaspercloud.sdwan.stun.StunPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class DataTest {

    public static void main(String[] args) throws Exception {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1001);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 1002);
        StunClient stunClient1 = new StunClient(address1);
        stunClient1.afterPropertiesSet();
        StunClient stunClient2 = new StunClient(address2);
        stunClient2.afterPropertiesSet();
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.Data, new BytesAttr("test".getBytes()));
        StunPacket request = new StunPacket(message, address2);
        stunClient1.getChannel().writeAndFlush(request);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

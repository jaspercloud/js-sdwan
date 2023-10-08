package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.AttrType;
import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.StunPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class P2pStunTest {

    public static void main(String[] args) throws Exception {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1001);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 1002);
        StunClient stunClient1 = new StunClient(address1, null);
        stunClient1.afterPropertiesSet();
        StunClient stunClient2 = new StunClient(address2, null);
        stunClient2.afterPropertiesSet();
        StunPacket stunPacket1 = stunClient1.sendBind(address2);
        System.out.println(stunPacket1.content().getAttrs().get(AttrType.MappedAddress));
        StunPacket stunPacket2 = stunClient2.sendBind(address1);
        System.out.println(stunPacket2.content().getAttrs().get(AttrType.MappedAddress));
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

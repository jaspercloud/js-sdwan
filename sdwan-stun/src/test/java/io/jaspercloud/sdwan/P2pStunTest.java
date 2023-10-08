package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.StunPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class P2pStunTest {

    public static void main(String[] args) throws Exception {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1001);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 1002);
        StunClient stunClient1 = StunClient.boot(address1);
        StunClient stunClient2 = StunClient.boot(address2);
        StunPacket stunPacket1 = stunClient1.sendBind(address2, 3000).get();
        StunPacket stunPacket2 = stunClient2.sendBind(address1, 3000).get();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

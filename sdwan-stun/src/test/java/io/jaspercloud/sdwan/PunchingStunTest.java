package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.StunMessage;
import io.jasercloud.sdwan.StunPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class PunchingStunTest {

    public static void main(String[] args) throws Exception {
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1001);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 1002);
        StunClient stunClient1 = new StunClient(address1);
        stunClient1.afterPropertiesSet();
        StunClient stunClient2 = new StunClient(address2);
        stunClient2.afterPropertiesSet();
        String tranId = StunMessage.genTranId();
        StunPacket stunPacket = stunClient2.sendPunchingBind(address1, tranId, 3000).get();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

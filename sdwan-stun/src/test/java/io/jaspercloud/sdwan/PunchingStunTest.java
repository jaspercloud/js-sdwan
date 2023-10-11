package io.jaspercloud.sdwan;

import java.net.InetSocketAddress;
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
        StunMessage message = new StunMessage(MessageType.BindRequest, tranId);
        StunPacket packet = new StunPacket(message, address1);
        StunPacket stunPacket = stunClient2.sendPunchingBind(packet, 3000).get();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

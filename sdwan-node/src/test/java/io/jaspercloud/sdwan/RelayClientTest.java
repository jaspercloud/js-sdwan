package io.jaspercloud.sdwan;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.RelayClient;
import io.jaspercloud.sdwan.stun.*;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class RelayClientTest {

    public static void main(String[] args) throws Exception {
        StunClient stunClient = new StunClient();
        stunClient.afterPropertiesSet();
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 888);
        RelayClient relayClient1 = new RelayClient(socketAddress, stunClient);
        relayClient1.afterPropertiesSet();
        RelayClient relayClient2 = new RelayClient(socketAddress, stunClient);
        relayClient2.afterPropertiesSet();
        Thread.sleep(3000);
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.setAttr(AttrType.RelayToken, new StringAttr(relayClient1.getRelayToken()));
        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.newBuilder()
                .setSrcIP("192.168.1.2")
                .setDstIP("192.168.1.3")
                .setPayload(SDWanProtos.RoutePacket.newBuilder()
                        .setSrcVIP("1")
                        .setDstVIP("2")
                        .setPayload(SDWanProtos.IpPacket.newBuilder()
                                .setSrcIP("1")
                                .setDstIP("2")
                                .setPayload(ByteString.EMPTY)
                                .build())
                        .build())
                .build();
        message.setAttr(AttrType.Data, new BytesAttr(p2pPacket.toByteArray()));
        relayClient2.send(message);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

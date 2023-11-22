package io.jaspercloud.sdwan;

public class RelayClientTest {

    public static void main(String[] args) throws Exception {
//        SDWanNodeProperties properties = new SDWanNodeProperties();
//        properties.setStunServer("stun.miwifi.com:3478");
//        properties.setRelayServer("192.222.0.66:51003");
//
//        StunClient stunClient = new StunClient(0);
//        stunClient.afterPropertiesSet();
//        RelayClient relayClient1 = new RelayClient(properties, stunClient);
//        relayClient1.afterPropertiesSet();
//        RelayClient relayClient2 = new RelayClient(properties, stunClient);
//        relayClient2.afterPropertiesSet();
//        Thread.sleep(3000);
//        StunMessage message = new StunMessage(MessageType.Transfer);
//        message.setAttr(AttrType.RelayToken, new StringAttr(relayClient1.getRelayToken()));
//        SDWanProtos.P2pPacket p2pPacket = SDWanProtos.P2pPacket.newBuilder()
//                .setSrcIP("192.168.1.2")
//                .setDstIP("192.168.1.3")
//                .setPayload(SDWanProtos.RoutePacket.newBuilder()
//                        .setSrcVIP("1")
//                        .setDstVIP("2")
//                        .setPayload(SDWanProtos.IpPacket.newBuilder()
//                                .setSrcIP("1")
//                                .setDstIP("2")
//                                .setPayload(ByteString.EMPTY)
//                                .build())
//                        .build())
//                .build();
//        message.setAttr(AttrType.Data, new BytesAttr(p2pPacket.toByteArray()));
//        relayClient2.send(message);
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
    }
}

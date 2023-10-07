//package io.jaspercloud.sdwan;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelHandlerContext;
//import org.drasyl.channel.tun.Tun4Packet;
//import org.junit.jupiter.api.Test;
//
//import java.io.ByteArrayOutputStream;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.util.concurrent.CountDownLatch;
//
//public class Ipv4DecodeTest {
//
//    @Test
//    public void test() throws Exception {
//        WinTun winTun = new WinTun("tun", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                Channel channel = ctx.channel();
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(packet.content());
//                ipv4Packet.setDstIP(InetAddress.getByName("10.1.0.50"));
//                TcpPacket tcpPacket = TcpPacket.decode(ipv4Packet.getPayload());
//                ByteBuf encodeTcp = tcpPacket.encode(ipv4Packet);
//                ipv4Packet.setPayload(encodeTcp);
//                Tun4Packet tun4Packet = new Tun4Packet(ipv4Packet.encode());
//                channel.writeAndFlush(tun4Packet);
//            }
//        });
//        winTun.start("10.1.0.10", 24);
//
//        while (true) {
//            try {
//                Socket socket = new Socket();
//                socket.connect(new InetSocketAddress("10.1.0.20", 80));
//            } catch (Exception e) {
//            }
//            System.out.println();
//        }
//    }
//
//    private byte[] toBytes(String data) {
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        char[] chars = data.toCharArray();
//        for (int i = 0; i < chars.length; i += 2) {
//            String s = String.valueOf(chars[i]) + String.valueOf(chars[i + 1]);
//            byte b = (byte) Integer.parseInt(s, 16);
//            stream.write(b);
//        }
//        byte[] bytes = stream.toByteArray();
//        return bytes;
//    }
//
//    private void showLog(ByteBuf byteBuf) {
//        byte[] bytes = new byte[byteBuf.readableBytes()];
//        byteBuf.readBytes(bytes);
//        for (byte b : bytes) {
//            System.out.print(String.format("0x%x", b));
//            System.out.print(" ");
//        }
//        System.out.println();
//    }
//}

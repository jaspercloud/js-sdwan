//package io.jaspercloud.sdwan;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import io.netty.channel.ChannelHandlerContext;
//import org.drasyl.channel.tun.Tun4Packet;
//import org.slf4j.impl.StaticLoggerBinder;
//
//import java.net.InetAddress;
//import java.util.concurrent.CountDownLatch;
//
//public class WinTunWriteTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//        Logger punching = loggerContext.getLogger("io.jaspercloud.sdwan");
//        punching.setLevel(Level.DEBUG);
//
//        WinTun winTun1 = new WinTun("tun1", new WinTun.TunnelDataHandler() {
//
//            public static final int TYPE = 20;
//            public static final int CHECKSUM = 22;
//            public static final int ECHO = 8;
//            public static final int ECHO_REPLY = 0;
//
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                short icmpType = packet.content().getUnsignedByte(TYPE);
//                if (icmpType == ECHO) {
//                    InetAddress source = packet.sourceAddress();
//                    InetAddress destination = packet.destinationAddress();
//                    System.out.println(String.format("tun1 in: %s->%s", source.getHostAddress(), destination.getHostAddress()));
//                    ctx.writeAndFlush(packet.retain());
//                }
//            }
//        });
//        winTun1.start("10.1.0.2", 24);
////        Thread.sleep(1000);
////        String data = "RQAAPDZ8AACAAfA+CgEAAgoBAAMIAErIAAECk2FiY2RlZmdoaWprbG1ub3BxcnN0dXZ3YWJjZGVmZ2hp";
////        while (true) {
////            byte[] bytes = Base64Utils.decodeFromString(data);
////            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
////            Tun4Packet packet = new Tun4Packet(byteBuf);
////            System.out.println(String.format("tun2 in: %s->%s",
////                    packet.sourceAddress().getHostAddress(), packet.destinationAddress().getHostAddress()));
////            winTun1.getChannel().writeAndFlush(packet);
////            Thread.sleep(1000);
////        }
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}

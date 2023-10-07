//package io.jaspercloud.sdwan;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import org.drasyl.channel.tun.Tun4Packet;
//import org.slf4j.impl.StaticLoggerBinder;
//
//import java.net.InetAddress;
//import java.util.concurrent.CountDownLatch;
//
//public class PingWinTunTest {
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
//                    ByteBuf buf = packet.content();
//                    int checksum = buf.getUnsignedShort(CHECKSUM);
//                    buf.setByte(TYPE, ECHO_REPLY);
//                    buf.setShort(CHECKSUM, checksum + 0x0800);
//                    Ipv4Packet decode = Ipv4Packet.decode(buf);
//                    InetAddress srcIP = decode.getSrcIP();
//                    InetAddress dstIP = decode.getDstIP();
//                    decode.setSrcIP(dstIP);
//                    decode.setDstIP(srcIP);
//                    ByteBuf encode = decode.encode();
//                    Tun4Packet response = new Tun4Packet(encode);
//                    ctx.writeAndFlush(response);
//                }
//            }
//        });
//        winTun1.start("10.1.0.2", 24);
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}

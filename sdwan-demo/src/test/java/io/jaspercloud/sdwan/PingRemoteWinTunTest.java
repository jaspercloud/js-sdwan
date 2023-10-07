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
//public class PingRemoteWinTunTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//        Logger punching = loggerContext.getLogger("io.jaspercloud.sdwan");
//        punching.setLevel(Level.DEBUG);
//
//        WinTun winTun1 = new WinTun("tun1", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                ByteBuf buf = packet.content();
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(buf);
//                InetAddress src = InetAddress.getByName("192.222.0.66");
//                InetAddress dst = InetAddress.getByName("192.222.8.153");
//                ipv4Packet.setSrcIP(src);
//                ipv4Packet.setDstIP(dst);
//                ByteBuf encode = ipv4Packet.encode();
//                Tun4Packet response = new Tun4Packet(encode);
//                ctx.writeAndFlush(response);
//            }
//        });
//        winTun1.start("10.1.0.1", 24);
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}

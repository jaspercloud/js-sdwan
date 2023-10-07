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
//
//public class WinTunServerTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//        Logger punching = loggerContext.getLogger("io.jaspercloud.sdwan");
//        punching.setLevel(Level.DEBUG);
//
//        WinTun winTun2 = new WinTun("tun2", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                System.out.println();
////                Ipv4Packet ipv4Packet = Ipv4Packet.decode(packet.content());
////                if ("10.2.0.1".equals(ipv4Packet.getDstIP().getHostAddress())) {
////                    InetAddress src = InetAddress.getByName("10.2.0.2");
////                    InetAddress dst = InetAddress.getByName("192.222.0.66");
////                    ipv4Packet.setSrcIP(src);
////                    ipv4Packet.setDstIP(dst);
////                    ByteBuf encode = ipv4Packet.encode();
////                    Tun4Packet tun4Packet = new Tun4Packet(encode);
////                    ctx.writeAndFlush(tun4Packet);
////                } else {
////                    System.out.println();
////                }
//            }
//        });
//        winTun2.start("10.2.0.5", 24);
//        WinTun winTun1 = new WinTun("tun1", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(packet.content());
//                if ("10.1.0.1".equals(ipv4Packet.getDstIP().getHostAddress())) {
//                    InetAddress src = InetAddress.getByName("10.2.0.2");
//                    InetAddress dst = InetAddress.getByName("10.2.0.5");
//                    ipv4Packet.setSrcIP(src);
//                    ipv4Packet.setDstIP(dst);
//                    ByteBuf encode = ipv4Packet.encode();
//                    Tun4Packet tun4Packet = new Tun4Packet(encode);
////                    ctx.writeAndFlush(tun4Packet);
//                    winTun2.writeAndFlush(tun4Packet);
//                } else {
//                    System.out.println();
//                }
//            }
//        });
//        winTun1.start("10.1.0.5", 24);
//    }
//}

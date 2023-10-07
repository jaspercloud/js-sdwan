//package io.jaspercloud.sdwan;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import org.drasyl.channel.tun.Tun4Packet;
//import org.slf4j.impl.StaticLoggerBinder;
//
//import java.util.concurrent.CountDownLatch;
//
//public class WInTunTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//
//        WinTun winTun = new WinTun("tun");
//        winTun.start("192.168.1.1", 24);
//        winTun.getChannel().pipeline().addLast(new ChannelInboundHandlerAdapter() {
//            @Override
//            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//                if (msg instanceof Tun4Packet) {
//                    Tun4Packet tun4Packet = (Tun4Packet) msg;
//                    ctx.writeAndFlush(tun4Packet);
//                }
//            }
//        });
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//
//}

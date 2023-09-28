package io.jasercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jasercloud.sdwan.support.UdpChannel;
import io.jaspercloud.sdwan.WinTun;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.drasyl.channel.tun.InetProtocol;
import org.drasyl.channel.tun.Tun4Packet;
import org.slf4j.impl.StaticLoggerBinder;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class ClientApp {

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        root.setLevel(Level.INFO);
        new ClientApp().run();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    private void run() throws Exception {
        InetSocketAddress address = new InetSocketAddress("192.222.8.159", 8888);
        Channel udpChannel = UdpChannel.newChannel(8888);
        WinTun winTun = new WinTun("tun");
        winTun.start("10.1.0.2", 24);
        winTun.getChannel().pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof Tun4Packet) {
                    Tun4Packet tun4Packet = (Tun4Packet) msg;
                    if (InetProtocol.TCP.decimal == tun4Packet.protocol()) {
                        System.out.println(String.format("W: %s", tun4Packet.destinationAddress().getHostAddress()));
                        DatagramPacket packet = new DatagramPacket(tun4Packet.content(), address);
                        udpChannel.writeAndFlush(packet);
                    } else {
                        ReferenceCountUtil.release(msg);
                    }
                } else {
                    ReferenceCountUtil.release(msg);
                }
            }
        });
        udpChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                DatagramPacket packet = (DatagramPacket) msg;
                Tun4Packet tun4Packet = new Tun4Packet(packet.content());
                System.out.println(String.format("R: %s", tun4Packet.sourceAddress().getHostAddress()));
                winTun.writeAndFlush(tun4Packet);
            }
        });
    }
}

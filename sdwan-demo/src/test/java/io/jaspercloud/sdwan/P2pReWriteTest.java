//package io.jaspercloud.sdwan;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import io.jasercloud.sdwan.Envelope;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.DatagramPacket;
//import io.netty.channel.socket.nio.NioDatagramChannel;
//import org.drasyl.channel.tun.Tun4Packet;
//import org.slf4j.impl.StaticLoggerBinder;
//
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//
//public class P2pReWriteTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//        Logger logger = loggerContext.getLogger("io.jaspercloud.sdwan");
//        logger.setLevel(Level.DEBUG);
//
//        InetSocketAddress remote = new InetSocketAddress("192.222.8.159", 8888);
//        Channel channel = createUdp(8888);
//        WinTun winTun = new WinTun("tun", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(packet.content());
//                ByteBuf encode = ipv4Packet.encode();
//                AddressedEnvelope<ByteBuf, InetSocketAddress> envelope = Envelope.<ByteBuf>builder()
//                        .message(encode)
//                        .recipient(remote)
//                        .toNettyEnvelope();
//                String src = ipv4Packet.getSrcIP().getHostAddress();
//                String dst = ipv4Packet.getDstIP().getHostAddress();
//                logger.debug("udp send: src={}, dest={}", src, dst);
//                channel.writeAndFlush(envelope);
//            }
//        });
//        winTun.start("10.1.0.20", 24);
//        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
//            @Override
//            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//                DatagramPacket packet = (DatagramPacket) msg;
//                ByteBuf byteBuf = packet.content();
//                Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
//                InetAddress src = InetAddress.getByName("192.222.0.66");
//                ipv4Packet.setSrcIP(src);
//                InetAddress dst = InetAddress.getByName("192.222.8.153");
//                ipv4Packet.setDstIP(dst);
//                ByteBuf encode = ipv4Packet.encode();
//                Tun4Packet tun4Packet = new Tun4Packet(encode);
//                String srcAddr = ipv4Packet.getSrcIP().getHostAddress();
//                String dstAddr = ipv4Packet.getDstIP().getHostAddress();
//                logger.debug("udp recv: src={}, dest={}", srcAddr, dstAddr);
//                winTun.writeAndFlush(tun4Packet);
//            }
//        });
//    }
//
//    public static Channel createUdp(int localPort) throws Exception {
//        NioEventLoopGroup group = new NioEventLoopGroup();
//        Bootstrap bootstrap = new Bootstrap();
//        bootstrap.group(group);
//        bootstrap.channel(NioDatagramChannel.class);
//        bootstrap.handler(new ChannelInitializer<Channel>() {
//            @Override
//            protected void initChannel(Channel ch) throws Exception {
//                ChannelPipeline pipeline = ch.pipeline();
//            }
//        });
//        Channel channel = bootstrap.bind(localPort).sync().channel();
//        channel.closeFuture().addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                group.shutdownGracefully();
//            }
//        });
//        return channel;
//    }
//}

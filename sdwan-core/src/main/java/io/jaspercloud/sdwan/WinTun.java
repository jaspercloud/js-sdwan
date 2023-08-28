package io.jaspercloud.sdwan;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.drasyl.AddressAndNetmaskHelper;
import org.drasyl.channel.tun.InetProtocol;
import org.drasyl.channel.tun.Tun4Packet;
import org.drasyl.channel.tun.TunAddress;
import org.drasyl.channel.tun.TunChannel;
import org.drasyl.channel.tun.jna.windows.WindowsTunDevice;
import org.drasyl.channel.tun.jna.windows.Wintun;

import java.io.IOException;

import static org.drasyl.channel.tun.jna.windows.Wintun.WintunGetAdapterLUID;

@Slf4j
public class WinTun {

    private String ifName;
    private TunnelDataHandler handler;
    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public WinTun(String ifName, TunnelDataHandler handler) {
        this.ifName = ifName;
        this.handler = handler;
    }

    public void start(String address, int netmaskPrefix) throws IOException {
        DefaultEventLoopGroup loopGroup = new DefaultEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(loopGroup)
                .channel(TunChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new TunWriteHandler(ifName));
                        pipeline.addLast(new TunReadHandler(ifName, handler));
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                                System.out.println(String.format("channelActive: %s", ifName));
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                System.out.println(String.format("channelInactive: %s", ifName));
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                super.exceptionCaught(ctx, cause);
                                System.out.println(String.format("exceptionCaught: %s", ifName));
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress(ifName));
        future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                loopGroup.shutdownGracefully();
            }
        });
        channel = future.syncUninterruptibly().channel();
        String name = channel.localAddress().toString();
        System.out.println("TUN device created: " + name);
        if (PlatformDependent.isOsx()) {
            exec("/sbin/ifconfig", name, "add", address, address);
            exec("/sbin/ifconfig", name, "up");
            exec("/sbin/route", "add", "-net", address + '/' + netmaskPrefix, "-iface", name);
        } else if (PlatformDependent.isWindows()) {
            // Windows
            final Wintun.WINTUN_ADAPTER_HANDLE adapter = ((WindowsTunDevice) ((TunChannel) channel).device()).adapter();
            final Pointer interfaceLuid = new Memory(8);
            WintunGetAdapterLUID(adapter, interfaceLuid);
            AddressAndNetmaskHelper.setIPv4AndNetmask(interfaceLuid, address, netmaskPrefix);
            exec(String.format("netsh interface ipv4 set subinterface \"%s\" mtu=1400 store=active", ifName));
        } else {
            // Linux
            exec(String.format("/sbin/ip addr add %s/%s dev %s", address, netmaskPrefix, ifName));
            exec(String.format("/sbin/ip link set dev %s up", ifName));
            exec(String.format("ifconfig %s mtu 1400 up", ifName));
        }
        System.out.println("Address and netmask assigned: " + address + '/' + netmaskPrefix);
    }

    private static class TunWriteHandler extends ChannelOutboundHandlerAdapter {

        private String ifName;

        public TunWriteHandler(String ifName) {
            this.ifName = ifName;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Tun4Packet) {
                Tun4Packet packet = (Tun4Packet) msg;
                String protocol = InetProtocol.protocolByDecimal(packet.protocol());
                String srcAddr = packet.sourceAddress().getHostAddress();
                String destAddr = packet.destinationAddress().getHostAddress();
                int readableBytes = packet.content().readableBytes();
                log.debug("send: ifName={}, protocol={}, src={}, dest={}, size={}",
                        ifName, protocol, srcAddr, destAddr, readableBytes);
                super.write(ctx, msg, promise);
            } else {
                super.write(ctx, msg, promise);
            }
        }
    }

    private static class TunReadHandler extends ChannelInboundHandlerAdapter {

        private String ifName;
        private TunnelDataHandler handler;

        public TunReadHandler(String ifName, TunnelDataHandler handler) {
            this.ifName = ifName;
            this.handler = handler;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Tun4Packet) {
                Tun4Packet packet = (Tun4Packet) msg;
                String protocol = InetProtocol.protocolByDecimal(packet.protocol());
                String srcAddr = packet.sourceAddress().getHostAddress();
                String destAddr = packet.destinationAddress().getHostAddress();
                if (destAddr.startsWith("224.0.0.")) {
                    return;
                }
                if ("10.1.0.255".equals(destAddr)) {
                    return;
                }
                if ("10.2.0.255".equals(destAddr)) {
                    return;
                }
                if ("239.255.255.250".equals(destAddr)) {
                    return;
                }
//                if (!Arrays.asList("TCP", "ICMP").contains(protocol)) {
//                    return;
//                }
                int readableBytes = packet.content().readableBytes();
                log.debug("recv: ifName={}, protocol={}, src={}, dest={}, size={}",
                        ifName, protocol, srcAddr, destAddr, readableBytes);
                handler.process(ctx, packet);
            } else {
                super.channelRead(ctx, msg);
            }
        }
    }

    public void writeAndFlush(Tun4Packet packet) {
        channel.writeAndFlush(packet);
    }

    public static void exec(String command) throws IOException {
        exec(command.split(" "));
    }

    private static void exec(String... command) throws IOException {
        try {
            final int exitCode = Runtime.getRuntime().exec(command).waitFor();
            if (exitCode != 0) {
                throw new IOException("Executing `" + String.join(" ", command) + "` returned non-zero exit code (" + exitCode + ").");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public interface TunnelDataHandler {

        void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception;
    }
}

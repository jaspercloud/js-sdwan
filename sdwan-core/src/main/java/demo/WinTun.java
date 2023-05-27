package demo;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import org.drasyl.AddressAndNetmaskHelper;
import org.drasyl.channel.tun.InetProtocol;
import org.drasyl.channel.tun.Tun4Packet;
import org.drasyl.channel.tun.TunAddress;
import org.drasyl.channel.tun.TunChannel;
import org.drasyl.channel.tun.jna.windows.WindowsTunDevice;
import org.drasyl.channel.tun.jna.windows.Wintun;

import java.io.IOException;

import static org.drasyl.channel.tun.jna.windows.Wintun.WintunGetAdapterLUID;

public class WinTun {

    private Channel channel;
    private String address;
    private int netmaskPrefix;

    public WinTun(String address, int netmaskPrefix) {
        this.address = address;
        this.netmaskPrefix = netmaskPrefix;
    }

    public void start(String ifName) throws IOException {
        DefaultEventLoopGroup loopGroup = new DefaultEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(loopGroup)
                .channel(TunChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Ping4Handler(address, WinTun.this));
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
        } else {
            // Linux
            exec("/sbin/ip", "addr", "add", address + '/' + netmaskPrefix, "dev", name);
            exec("/sbin/ip", "link", "set", "dev", name, "up");
        }
        System.out.println("Address and netmask assigned: " + address + '/' + netmaskPrefix);
    }

    private static class Ping4Handler extends SimpleChannelInboundHandler<Tun4Packet> {

        private String address;
        private WinTun winTun;

        public Ping4Handler(String address, WinTun winTun) {
            this.address = address;
            this.winTun = winTun;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            WinTunManager.addWinTun(address, winTun);
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx,
                                    final Tun4Packet packet) {
            if (packet.protocol() == InetProtocol.ICMP.decimal) {
            } else if (packet.protocol() == InetProtocol.TCP.decimal) {
                System.out.println(String.format("rec: protocol=%s, src=%s, dest=%s",
                        InetProtocol.protocolByDecimal(packet.protocol()).toString(),
                        packet.sourceAddress().getHostAddress(),
                        packet.destinationAddress().getHostAddress()));
                WinTun winTun = WinTunManager.getWinTun(packet.destinationAddress().getHostAddress());
                winTun.write(packet);
            } else {
                ctx.fireChannelRead(packet.retain());
            }
        }
    }

    private void write(Tun4Packet packet) {
        channel.writeAndFlush(packet.retain());
    }

    private static void exec(final String... command) throws IOException {
        try {
            final int exitCode = Runtime.getRuntime().exec(command).waitFor();
            if (exitCode != 0) {
                throw new IOException("Executing `" + String.join(" ", command) + "` returned non-zero exit code (" + exitCode + ").");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

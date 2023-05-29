package io.jaspercloud.sdwan;

import com.google.protobuf.ByteString;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
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
                        pipeline.addLast(new TunProcessHandler(ifName, handler));
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
            exec(String.format("netsh interface ipv4 set subinterface \"%s\" mtu=1400 store=persistent", ifName));
        } else {
            // Linux
            exec(String.format("/sbin/ip addr add %s/%s dev %s", address, netmaskPrefix, ifName));
            exec(String.format("/sbin/ip link set dev %s up", ifName));
            exec(String.format("ifconfig %s mtu 1400 up", ifName));
        }
        System.out.println("Address and netmask assigned: " + address + '/' + netmaskPrefix);
    }

    private static class TunProcessHandler extends SimpleChannelInboundHandler<Tun4Packet> {

        private String ifName;
        private TunnelDataHandler handler;

        public TunProcessHandler(String ifName, TunnelDataHandler handler) {
            this.ifName = ifName;
            this.handler = handler;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx,
                                    final Tun4Packet packet) {
            if (packet.protocol() != InetProtocol.TCP.decimal) {
                return;
            }
            String srcAddr = packet.sourceAddress().getHostAddress();
            String destAddr = packet.destinationAddress().getHostAddress();
            int readableBytes = packet.content().readableBytes();
            log.debug("recv: ifName={}, src={}, dest={}, size={}", ifName, srcAddr, destAddr, readableBytes);
            byte[] bytes = new byte[readableBytes];
            packet.content().readBytes(bytes);
            SDWanProtos.UdpTunnelData tunnelData = SDWanProtos.UdpTunnelData.newBuilder()
                    .setType(SDWanProtos.MsgType.TunnelData)
                    .setSrc(srcAddr)
                    .setDest(destAddr)
                    .setData(ByteString.copyFrom(bytes))
                    .build();
            handler.process(tunnelData);
        }
    }

    public void write(Tun4Packet packet) {
        log.debug("send: ifName={}, dest={} size={}", ifName, packet.destinationAddress().getHostAddress(), packet.content().readableBytes());
        channel.writeAndFlush(packet);
    }

    private static void exec(String command) throws IOException {
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

        void process(SDWanProtos.UdpTunnelData tunnelData);
    }
}

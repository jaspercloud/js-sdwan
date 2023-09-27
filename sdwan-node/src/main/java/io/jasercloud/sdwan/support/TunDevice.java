package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.support.transporter.Transporter;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TunDevice implements InitializingBean, Runnable {

    public static final String TUN = "tun";

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private Transporter transporter;

    private TunChannel tunChannel;

    public TunDevice(SDWanNodeProperties properties, SDWanNode sdWanNode, Transporter transporter) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.transporter = transporter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        transporter.setReceiveHandler(new Transporter.ReceiveHandler() {
            @Override
            public void onPacket(IpPacket ipPacket) {
                if (null != tunChannel) {
                    Ipv4Packet ipv4Packet = (Ipv4Packet) ipPacket;
                    tunChannel.writeAndFlush(ipv4Packet);
                }
            }
        });
        Thread thread = new Thread(this, "sdwan-device");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                tunChannel = bootTunDevices();
                SDWanProtos.RegResp regResp = sdWanNode.regist(3000);
                log.info("tunAddress: {}/{}", regResp.getVip(), regResp.getMaskBits());
                tunChannel.setAddress(regResp.getVip(), regResp.getMaskBits());
                //等待ip设置成功，再配置路由
                waitAddress(regResp.getVip(), 15000);
                addRoutes(regResp.getVip());
                try {
                    sdWanNode.getChannel().closeFuture().sync();
                } finally {
                    tunChannel.close().sync();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private TunChannel bootTunDevices() {
        Bootstrap bootstrap = new Bootstrap()
                .group(new DefaultEventLoopGroup())
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, properties.getMtu())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                Ipv4Packet ipv4Packet = Ipv4Packet.decode(msg);
                                transporter.writePacket(ipv4Packet);
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress(TUN));
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        return tunChannel;
    }

    private void waitAddress(String vip, int timeout) throws Exception {
        long s = System.currentTimeMillis();
        while (true) {
            NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(vip);
            if (null != networkInterfaceInfo) {
                return;
            }
            long e = System.currentTimeMillis();
            long diff = e - s;
            if (diff > timeout) {
                throw new TimeoutException();
            }
        }
    }

    private void addRoutes(String vip) {
        List<String> routes = properties.getRoutes();
        routes.forEach(route -> {
            try {
                tunChannel.addRoute(route, vip);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }
}

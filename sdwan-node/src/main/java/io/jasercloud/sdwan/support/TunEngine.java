package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.CheckResult;
import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.support.transporter.Transporter;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.TunAddress;
import io.jasercloud.sdwan.tun.TunChannel;
import io.jasercloud.sdwan.tun.TunChannelConfig;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class TunEngine implements InitializingBean, DisposableBean, Runnable {

    public static final String TUN = "tun";

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private Transporter transporter;
    private NatManager natManager;
    private StunClient stunClient;

    private TunChannel tunChannel;

    public TunEngine(SDWanNodeProperties properties,
                     SDWanNode sdWanNode,
                     Transporter transporter,
                     NatManager natManager,
                     StunClient stunClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.transporter = transporter;
        this.natManager = natManager;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        tunChannel = bootTunDevices();
        transporter.setReceiveHandler(new Transporter.ReceiveHandler() {
            @Override
            public void onPacket(ByteBuf byteBuf) {
                natManager.input(tunChannel, byteBuf);
            }
        });
        Thread thread = new Thread(this, "tun-device");
        thread.start();
    }

    @Override
    public void destroy() throws Exception {
        tunChannel.close().sync();
    }

    @Override
    public void run() {
        while (true) {
            try {
                CheckResult checkResult = stunClient.getSelfCheckResult();
                SDWanProtos.RegResp regResp = sdWanNode.regist(
                        checkResult.getMapping(),
                        checkResult.getFiltering(),
                        checkResult.getMappingAddress(),
                        5000
                );
                if (SDWanProtos.MessageCode.NodeTypeError_VALUE == regResp.getCode()) {
                    throw new ProcessException("meshNode must staticNode");
                } else if (SDWanProtos.MessageCode.NodeTypeError_VALUE == regResp.getCode()) {
                    throw new ProcessException("no more vip");
                }
                log.info("tunAddress: {}/{}", regResp.getVip(), regResp.getMaskBits());
                tunChannel.setAddress(regResp.getVip(), regResp.getMaskBits());
                //等待ip设置成功，再配置路由
                waitAddress(regResp.getVip(), 15000);
                List<String> routes = regResp.getRouteList()
                        .stream()
                        .collect(Collectors.toList());
                NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(regResp.getVip());
                addRoutes(interfaceInfo, regResp.getVip(), routes);
                sdWanNode.getChannel().closeFuture().sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private TunChannel bootTunDevices() {
        DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, properties.getMtu())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                Ipv4Packet packet = Ipv4Packet.decodeMark(msg);
                                if (4 != packet.getVersion()) {
                                    return;
                                }
                                natManager.output(sdWanNode, transporter, msg.retain());
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress(TUN));
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        tunChannel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                eventLoopGroup.shutdownGracefully();
            }
        });
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

    private void addRoutes(NetworkInterfaceInfo interfaceInfo, String vip, List<String> routes) {
        routes.forEach(route -> {
            try {
                log.info("addRoute: {} -> {}", route, vip);
                tunChannel.addRoute(interfaceInfo, route, vip);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }
}

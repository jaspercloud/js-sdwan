package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.CheckResult;
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
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class TunEngine implements InitializingBean, DisposableBean, Runnable {

    public static final String TUN = "tun";

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private Transporter transporter;
    private PunchingManager punchingManager;
    private SDArpManager sdArpManager;

    private TunChannel tunChannel;

    public TunEngine(SDWanNodeProperties properties,
                     SDWanNode sdWanNode,
                     Transporter transporter,
                     PunchingManager punchingManager,
                     SDArpManager sdArpManager) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.transporter = transporter;
        this.punchingManager = punchingManager;
        this.sdArpManager = sdArpManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        tunChannel = bootTun();
        transporter.bind(tunChannel);
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
                CheckResult checkResult = punchingManager.getCheckResult();
                SDWanProtos.RegResp regResp;
                try {
                    regResp = sdWanNode.regist(
                            checkResult.getMapping(),
                            checkResult.getFiltering(),
                            checkResult.getMappingAddress(),
                            5000
                    );
                } catch (TimeoutException e) {
                    throw new ProcessException("sdWANNode.regist timeout");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof TimeoutException) {
                        throw new ProcessException("sdWANNode.regist timeout");
                    }
                    throw e;
                }
                if (SDWanProtos.MessageCode.NodeTypeError_VALUE == regResp.getCode()) {
                    throw new ProcessException("meshNode must staticNode");
                } else if (SDWanProtos.MessageCode.NodeTypeError_VALUE == regResp.getCode()) {
                    throw new ProcessException("no more vip");
                }
                //配置地址
                tunChannel.setAddress(regResp.getVip(), regResp.getMaskBits());
                log.info("tunAddress: {}/{}", regResp.getVip(), regResp.getMaskBits());
                //配置路由
                List<String> routes = regResp.getRouteList()
                        .stream()
                        .collect(Collectors.toList());
                NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(regResp.getVip());
                addRoutes(interfaceInfo, regResp.getVip(), routes);
                //wait closed reconnect
                sdWanNode.getChannel().closeFuture().sync();
            } catch (ProcessException e) {
                log.error(e.getMessage(), e);
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

    private TunChannel bootTun() {
        DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, properties.getMtu())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                TunAddress tunAddress = (TunAddress) ctx.channel().localAddress();
                                msg.markReaderIndex();
                                int version = (msg.readUnsignedByte() >> 4);
                                if (4 != version) {
                                    return;
                                }
                                msg.resetReaderIndex();

                                String localVIP = tunAddress.getVip();
                                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(msg);
                                ByteBuf byteBuf = msg.retain();
                                sdArpManager.sdArp(sdWanNode, localVIP, ipv4Packet)
                                        .whenComplete((address, throwable) -> {
                                            if (null != throwable) {
                                                log.error("sdArpTimeout: {}", ipv4Packet.getDstIP());
                                                byteBuf.release();
                                                return;
                                            }
                                            if (null == address) {
                                                byteBuf.release();
                                                return;
                                            }
                                            DatagramPacket packet = new DatagramPacket(byteBuf, address);
                                            ctx.fireChannelRead(packet);
                                        });
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress(TUN));
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        return tunChannel;
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

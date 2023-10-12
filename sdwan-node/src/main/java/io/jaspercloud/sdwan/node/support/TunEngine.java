package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.transporter.Transporter;
import io.jaspercloud.sdwan.stun.CheckResult;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.TunChannelConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class TunEngine implements InitializingBean, DisposableBean, Runnable {

    public static final String TUN = "sdwan";

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private Transporter transporter;
    private PunchingManager punchingManager;
    private SDArpManager sdArpManager;

    private AtomicReference<List<SDWanProtos.Route>> routeCache = new AtomicReference<>();

    private NetworkInterfaceInfo interfaceInfo;
    private TunChannel tunChannel;

    public TunChannel getTunChannel() {
        return tunChannel;
    }

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
        interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(properties.getLocalIP());
        if (null == interfaceInfo) {
            throw new ProcessException("not found localIP");
        }
        tunChannel = bootTun();
        transporter.bind(tunChannel);
        sdWanNode.addDataHandler(new SDWanDataHandler<SDWanProtos.RouteList>() {
            @Override
            public void onData(ChannelHandlerContext ctx, SDWanProtos.RouteList msg) throws Exception {
                TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
                String localVIP = tunAddress.getVip();
                List<SDWanProtos.Route> routeList = msg.getRouteList();
                updateRoutes(localVIP, routeList);
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
                CheckResult checkResult = punchingManager.getCheckResult();
                SDWanProtos.RegResp regResp;
                try {
                    regResp = sdWanNode.regist(
                            interfaceInfo,
                            checkResult.getLocalPort(),
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
                } else if (SDWanProtos.MessageCode.VipBound_VALUE == regResp.getCode()) {
                    throw new ProcessException("vip bound");
                }
                //配置地址
                tunChannel.setAddress(regResp.getVip(), regResp.getMaskBits());
                log.info("tunAddress: {}/{}", regResp.getVip(), regResp.getMaskBits());
                //配置路由
                addRoutes(regResp.getVip(), regResp.getRouteList().getRouteList());
                log.info("TunEngine started");
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

    private TunChannel bootTun() throws Exception {
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
                        pipeline.addLast("TunEngine:readTun", new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                TunAddress tunAddress = (TunAddress) ctx.channel().localAddress();
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
        ChannelFuture future = bootstrap.bind(new TunAddress(TUN, interfaceInfo.getName()));
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        return tunChannel;
    }

    private void addRoutes(String vip, List<SDWanProtos.Route> routeList) throws SocketException {
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(vip);
        routeList = routeList.stream()
                .filter(e -> !StringUtils.equals(e.getNexthop(), vip))
                .collect(Collectors.toList());
        routeList.forEach(route -> {
            try {
                log.info("addRoute: {} -> {}", route.getDestination(), vip);
                tunChannel.addRoute(interfaceInfo, route.getDestination(), vip);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
        routeCache.set(routeList);
    }

    private void updateRoutes(String vip, List<SDWanProtos.Route> routeList) throws SocketException {
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(vip);
        List<SDWanProtos.Route> currentList = routeCache.get();
        if (null != currentList) {
            currentList.forEach(route -> {
                try {
                    log.info("delRoute: {} -> {}", route.getDestination(), vip);
                    tunChannel.delRoute(interfaceInfo, route.getDestination(), vip);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
        addRoutes(vip, routeList);
    }
}

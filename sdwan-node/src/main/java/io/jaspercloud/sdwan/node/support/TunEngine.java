package io.jaspercloud.sdwan.node.support;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.detection.AddressType;
import io.jaspercloud.sdwan.node.node.MappingManager;
import io.jaspercloud.sdwan.node.node.RelayClient;
import io.jaspercloud.sdwan.node.node.SDWanNode;
import io.jaspercloud.sdwan.node.route.RouteManager;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.TunChannelConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TunEngine implements InitializingBean, DisposableBean, Runnable {

    public static final String TUN = "net-thunder";

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private RelayClient relayClient;
    private MappingManager mappingManager;
    private RouteManager routeManager;
    private ConnectionManager connectionManager;

    private TunChannel tunChannel;

    public TunChannel getTunChannel() {
        return tunChannel;
    }

    public TunEngine(SDWanNodeProperties properties,
                     SDWanNode sdWanNode,
                     StunClient stunClient,
                     RelayClient relayClient,
                     MappingManager mappingManager,
                     RouteManager routeManager,
                     ConnectionManager connectionManager) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.relayClient = relayClient;
        this.mappingManager = mappingManager;
        this.routeManager = routeManager;
        this.connectionManager = connectionManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        tunChannel = bootTun();
        routeManager.addUpdateRouteHandler(routeList -> {
            routeManager.updateRouteList(tunChannel, routeList);
        });
        connectionManager.addConnectionDataHandler((connection, packet) -> {
            try {
                processWriteTun(tunChannel, packet);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
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
            if (sdWanNode.getChannel().isActive()) {
                autoBoot();
            }
            try {
                Thread.sleep(5000);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void autoBoot() {
        try {
            InetSocketAddress sdWanNodeLocalAddress = (InetSocketAddress) sdWanNode.getChannel().localAddress();
            InetSocketAddress stunClientLocalAddress = (InetSocketAddress) stunClient.getChannel().localAddress();
            MappingAddress mappingAddress = mappingManager.getMappingAddress();
            //address
            String host = UriComponentsBuilder.newInstance()
                    .scheme(AddressType.HOST)
                    .host(sdWanNodeLocalAddress.getHostString())
                    .port(stunClientLocalAddress.getPort())
                    .build().toString();
            String srflx = UriComponentsBuilder.newInstance()
                    .scheme(AddressType.SRFLX)
                    .host(mappingAddress.getMappingAddress().getHostString())
                    .port(mappingAddress.getMappingAddress().getPort())
                    .queryParam("mappingType", mappingAddress.getMappingType().name())
                    .build().toString();
            String relay = UriComponentsBuilder.newInstance()
                    .scheme(AddressType.RELAY)
                    .host(properties.getRelay().getAddress().getHostString())
                    .port(properties.getRelay().getAddress().getPort())
                    .queryParam("token", relayClient.getRelayToken())
                    .build().toString();
            NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(sdWanNodeLocalAddress.getHostString());
            String hardwareAddress = interfaceInfo.getHardwareAddress();
            SDWanProtos.RegResp regResp;
            try {
                regResp = sdWanNode.regist(hardwareAddress, Arrays.asList(host, srflx, relay));
            } catch (TimeoutException e) {
                throw new ProcessException("sdWANNode.regist timeout");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof TimeoutException) {
                    throw new ProcessException("sdWANNode.regist timeout");
                }
                throw e;
            }
            if (SDWanProtos.MessageCode.NotEnough_VALUE == regResp.getCode()) {
                throw new ProcessException("no more vip");
            } else if (SDWanProtos.MessageCode.VipBound_VALUE == regResp.getCode()) {
                throw new ProcessException("vip bound");
            } else if (SDWanProtos.MessageCode.SysError_VALUE == regResp.getCode()) {
                throw new ProcessException("server error");
            }
            //配置地址
            tunChannel.setAddress(regResp.getVip(), regResp.getMaskBits());
            log.info("tunAddress: {}/{}", regResp.getVip(), regResp.getMaskBits());
            //配置路由
            routeManager.initRoute(tunChannel);
            log.info("TunEngine started");
            //wait closed reconnect
            sdWanNode.getChannel().closeFuture().sync();
            routeManager.releaseRoute(tunChannel);
        } catch (ProcessException e) {
            log.error(e.getMessage(), e);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private TunChannel bootTun() throws Exception {
        InetSocketAddress localAddress = (InetSocketAddress) sdWanNode.getChannel().localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(localAddress.getHostString());
        DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, properties.getTun().getMtu())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("TunEngine:readTun", new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                processReadTun(ctx, msg);
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress(TUN, interfaceInfo.getName()));
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        return tunChannel;
    }

    private void processReadTun(ChannelHandlerContext ctx, ByteBuf msg) {
        TunAddress tunAddress = (TunAddress) ctx.channel().localAddress();
        String localVIP = tunAddress.getVip();
        if (StringUtils.isEmpty(localVIP)) {
            //init
            return;
        }
        Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(msg);
        SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.newBuilder()
                .setSrcIP(ipv4Packet.getSrcIP())
                .setDstIP(ipv4Packet.getDstIP())
                .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(msg)))
                .build();
        if (ignoreIp(ipPacket.getDstIP())) {
            return;
        }
        SDWanProtos.RoutePacket routePacket = routeManager.routeOut(localVIP, ipPacket);
        if (null == routePacket) {
            return;
        }
        log.debug("tun read: src={}, dst={}", ipPacket.getSrcIP(), ipPacket.getDstIP());
        connectionManager.send(routePacket);
    }

    private void processWriteTun(TunChannel tunChannel, SDWanProtos.RoutePacket routePacket) {
        SDWanProtos.IpPacket ipPacket = routeManager.routeIn(routePacket);
        if (null == ipPacket) {
            return;
        }
        if (ignoreIp(ipPacket.getDstIP())) {
            return;
        }
        log.debug("tun write: src={}, dst={}", ipPacket.getSrcIP(), ipPacket.getDstIP());
        tunChannel.writeAndFlush(ByteBufUtil.toByteBuf(ipPacket.getPayload().toByteArray()));
    }

    private boolean ignoreIp(String ip) {
        List<String> ignoreIpList = properties.getTun().getIgnoreIpList();
        if (null == ignoreIpList) {
            return false;
        }
        boolean contains = ignoreIpList.contains(ip);
        return contains;
    }
}

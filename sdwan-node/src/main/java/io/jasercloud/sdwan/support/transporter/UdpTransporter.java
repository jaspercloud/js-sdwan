package io.jasercloud.sdwan.support.transporter;

import io.jasercloud.sdwan.support.SDWanNode;
import io.jasercloud.sdwan.support.Transporter;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.IpPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UdpTransporter implements InitializingBean, Transporter {

    private SDWanNode sdWanNode;
    private ReceiveHandler handler;
    private Channel channel;

    private Map<String, SDWanProtos.SDArpResp> arpCache = new ConcurrentHashMap<>();

    public UdpTransporter(SDWanNode sdWanNode) {
        this.sdWanNode = sdWanNode;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                        if (null != handler) {
                            Ipv4Packet ipv4Packet = Ipv4Packet.decode(msg.content());
                            handler.onPacket(ipv4Packet);
                        }
                    }
                });
            }
        });
        channel = bootstrap.bind(0).sync().channel();
    }

    @Override
    public void writePacket(IpPacket ipPacket) {
        try {
            SDWanProtos.SDArpResp sdArp = arpCache.get(ipPacket.getDstIP());
            if (null == sdArp) {
                sdArp = sdWanNode.sdArp(ipPacket.getDstIP().getHostAddress(), 3000);
                if (SDWanProtos.MessageCode.Success_VALUE != sdArp.getCode()) {
                    return;
                }
                arpCache.put(ipPacket.getDstIP().getHostAddress(), sdArp);
            }
            String publicIP = sdArp.getPublicIP();
            int publicPort = sdArp.getPublicPort();
            InetSocketAddress address = new InetSocketAddress(publicIP, publicPort);
            Ipv4Packet ipv4Packet = (Ipv4Packet) ipPacket;
            DatagramPacket packet = new DatagramPacket(ipv4Packet.encode(), address);
            channel.writeAndFlush(packet);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void setReceiveHandler(ReceiveHandler handler) {
        this.handler = handler;
    }
}

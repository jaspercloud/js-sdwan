package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.*;
import io.jaspercloud.sdwan.WinTun;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.drasyl.channel.tun.Tun4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties) {
        return new SDWanNode(properties);
    }

    @Bean
    public SDWanNodeInfoManager nodeManager() {
        return new SDWanNodeInfoManager();
    }

    @Bean
    public UdpNode udpNode(SDWanNodeProperties properties,
                           ObjectProvider<WinTun> provider) {
        return new UdpNode(properties.getNodeUdpPort(), new UdpNode.UdpProcessHandler() {

            private Logger log = LoggerFactory.getLogger(UdpNode.class);

            @Override
            public void process(ChannelHandlerContext ctx, DatagramPacket packet) {
                try {
                    WinTun winTun = provider.getIfAvailable();
                    int readableBytes = packet.content().readableBytes();
                    byte[] buf = new byte[readableBytes];
                    packet.content().readBytes(buf);
                    SDWanProtos.UdpTunnelData tunnelData = SDWanProtos.UdpTunnelData.parseFrom(buf);
                    byte[] bytes = tunnelData.getData().toByteArray();
                    ByteBuf buffer = ctx.channel().alloc().buffer(bytes.length);
                    buffer.writeBytes(bytes);
                    Tun4Packet tun4Packet = new Tun4Packet(buffer);
                    log.debug("parse Tun4Packet: size={} src={}, dest={}",
                            bytes.length, tun4Packet.sourceAddress().getHostAddress(), tun4Packet.destinationAddress().getHostAddress());
                    winTun.write(tun4Packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Bean
    public WinTun winTun(SDWanNodeProperties properties,
                         SDWanNodeInfoManager nodeManager,
                         ObjectProvider<UdpNode> provider) {
        return new WinTun(properties.getNodeId(), new WinTun.TunnelDataHandler() {
            @Override
            public void process(SDWanProtos.UdpTunnelData tunnelData) {
                UdpNode udpNode = provider.getIfAvailable();
                String dest = tunnelData.getDest();
                SDWanNodeInfo nodeInfo = nodeManager.get(dest);
                if (null == nodeInfo) {
                    return;
                }
                udpNode.write(nodeInfo.getNodeIP(), nodeInfo.getNodeUdpPort(), tunnelData.toByteArray());
            }
        });
    }
}

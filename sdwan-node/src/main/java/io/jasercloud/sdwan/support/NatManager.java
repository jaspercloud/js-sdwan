package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.support.transporter.Transporter;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.IpPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NatManager {

    private Map<String, SDWanProtos.SDArpResp> arpCache = new ConcurrentHashMap<>();

    public void output(SDWanNode sdWanNode, Transporter transporter, IpPacket ipPacket) {
        try {
            String ip = ipPacket.getDstIP().getHostAddress();
            SDWanProtos.SDArpResp sdArp = arpCache.get(ip);
            if (null == sdArp) {
                sdArp = sdWanNode.sdArp(ipPacket.getDstIP().getHostAddress(), 3000);
                if (SDWanProtos.MessageCode.Success_VALUE != sdArp.getCode()) {
                    return;
                }
                arpCache.put(ip, sdArp);
            }
            String publicIP = sdArp.getPublicIP();
            int publicPort = sdArp.getPublicPort();
            InetSocketAddress address = new InetSocketAddress(publicIP, publicPort);
            Ipv4Packet ipv4Packet = (Ipv4Packet) ipPacket;
            ByteBuf byteBuf = ipv4Packet.encode();
            transporter.writePacket(address, byteBuf);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void input(Channel channel, IpPacket ipPacket) {
        try {
            channel.writeAndFlush(ipPacket);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}

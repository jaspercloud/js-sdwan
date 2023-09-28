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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
public class NatManager {

    private Map<String, SDWanProtos.SDArpResp> arpCache = new ConcurrentHashMap<>();

    public void output(SDWanNode sdWanNode, Transporter transporter, IpPacket ipPacket) {
        String ip = ipPacket.getDstIP().getHostAddress();
        CompletableFuture.supplyAsync(() -> {
            SDWanProtos.SDArpResp sdArp = arpCache.get(ip);
            return sdArp;
        }).thenComposeAsync(new Function<SDWanProtos.SDArpResp, CompletionStage<SDWanProtos.SDArpResp>>() {
            @Override
            public CompletionStage<SDWanProtos.SDArpResp> apply(SDWanProtos.SDArpResp sdArp) {
                if (null == sdArp) {
                    return sdWanNode.sdArp(ipPacket.getDstIP().getHostAddress(), 3000);
                }
                return CompletableFuture.completedFuture(sdArp);
            }
        }).thenApply(sdArp -> {
            if (SDWanProtos.MessageCode.Success_VALUE != sdArp.getCode()) {
                return null;
            }
            arpCache.put(ip, sdArp);
            return sdArp;
        }).thenAccept(sdArp -> {
            if (null == sdArp) {
                return;
            }
            String publicIP = sdArp.getPublicIP();
            int publicPort = sdArp.getPublicPort();
            System.out.println(String.format("output: %s -> %s", ipPacket.getSrcIP(), ipPacket.getDstIP()));
            InetSocketAddress address = new InetSocketAddress(publicIP, publicPort);
            Ipv4Packet ipv4Packet = (Ipv4Packet) ipPacket;
            ByteBuf byteBuf = ipv4Packet.encode();
            transporter.writePacket(address, byteBuf);
        });
    }

    public void input(Channel tunChannel, IpPacket ipPacket) {
        try {
            System.out.println(String.format("input: %s -> %s", ipPacket.getSrcIP(), ipPacket.getDstIP()));
            Ipv4Packet ipv4Packet = (Ipv4Packet) ipPacket;
            tunChannel.writeAndFlush(ipv4Packet.encode());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}

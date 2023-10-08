package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.support.transporter.Transporter;
import io.jasercloud.sdwan.tun.IpPacket;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class NatManager {

    public Timer TIMEOUT = new HashedWheelTimer(
            new DefaultThreadFactory("arp-timeout", true),
            30, TimeUnit.MILLISECONDS);
    private Map<String, SDWanProtos.SDArpResp> arpCache = new ConcurrentHashMap<>();

    private NodeManager nodeManager;

    public NatManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void output(SDWanNode sdWanNode, Transporter transporter, IpPacket ipPacket) {
        String ip = ipPacket.getDstIP();
        CompletableFuture.supplyAsync(() -> {
            SDWanProtos.SDArpResp sdArp = arpCache.get(ip);
            return sdArp;
        }).thenComposeAsync(new Function<SDWanProtos.SDArpResp, CompletionStage<SDWanProtos.SDArpResp>>() {
            @Override
            public CompletionStage<SDWanProtos.SDArpResp> apply(SDWanProtos.SDArpResp sdArp) {
                if (null == sdArp) {
                    return sdWanNode.sdArp(ipPacket.getDstIP(), 3000);
                }
                return CompletableFuture.completedFuture(sdArp);
            }
        }).thenApply(sdArp -> {
            if (SDWanProtos.MessageCode.Success_VALUE != sdArp.getCode()) {
                return null;
            }
            arpCache.put(ip, sdArp);
            TIMEOUT.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    arpCache.remove(ip);
                }
            }, sdArp.getTtl(), TimeUnit.SECONDS);
            return sdArp;
        }).thenComposeAsync(sdArp -> {
            if (null == sdArp) {
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<InetSocketAddress> future = nodeManager.getPublicAddress(sdArp);
            return future;
        }).thenAccept(address -> {
            if (null == address) {
                return;
            }
            System.out.println(String.format("output: %s -> %s next %s", ipPacket.getSrcIP(), ipPacket.getDstIP(), address));
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

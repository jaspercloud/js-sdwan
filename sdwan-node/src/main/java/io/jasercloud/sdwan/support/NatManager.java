package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.support.transporter.Transporter;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ReferenceCountUtil;
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

    private PunchingManager punchingManager;

    public NatManager(PunchingManager punchingManager) {
        this.punchingManager = punchingManager;
    }

    public void output(SDWanNode sdWanNode, Transporter transporter, ByteBuf byteBuf) {
        Ipv4Packet packet = Ipv4Packet.decodeMark(byteBuf);
        String ip = packet.getDstIP();
        CompletableFuture.supplyAsync(() -> {
            SDWanProtos.SDArpResp sdArp = arpCache.get(ip);
            return sdArp;
        }).thenComposeAsync(new Function<SDWanProtos.SDArpResp, CompletionStage<SDWanProtos.SDArpResp>>() {
            @Override
            public CompletionStage<SDWanProtos.SDArpResp> apply(SDWanProtos.SDArpResp sdArp) {
                if (null == sdArp) {
                    return sdWanNode.sdArp(packet.getDstIP(), 3000);
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
            CompletableFuture<InetSocketAddress> future = punchingManager.getPublicAddress(sdArp);
            return future;
        }).whenComplete((address, throwable) -> {
            if (null != throwable) {
                ReferenceCountUtil.release(byteBuf);
                return;
            }
            if (null == address) {
                ReferenceCountUtil.release(byteBuf);
                return;
            }
            System.out.println(String.format("output: %s -> %s next %s", packet.getSrcIP(), packet.getDstIP(), address));
            transporter.writePacket(address, byteBuf);
        });
    }

    public void input(Channel tunChannel, ByteBuf byteBuf) {
        try {
            Ipv4Packet packet = Ipv4Packet.decodeMark(byteBuf);
            System.out.println(String.format("input: %s -> %s", packet.getSrcIP(), packet.getDstIP()));
            tunChannel.writeAndFlush(byteBuf);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}

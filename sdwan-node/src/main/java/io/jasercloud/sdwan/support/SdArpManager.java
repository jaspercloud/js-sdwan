package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.tun.IpPacket;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class SdArpManager {

    public Timer TIMEOUT = new HashedWheelTimer(
            new DefaultThreadFactory("sd-arp-timeout", true),
            20, TimeUnit.MILLISECONDS);
    private Map<String, SDWanProtos.SDArpResp> sdArpCache = new ConcurrentHashMap<>();

    private PunchingManager punchingManager;

    public SdArpManager(PunchingManager punchingManager) {
        this.punchingManager = punchingManager;
    }

    @EventListener(NodeOfflineEvent.class)
    public void onNodeOfflineEvent(NodeOfflineEvent event) {
        sdArpCache.remove(event.getVip());
    }

    public CompletableFuture<InetSocketAddress> sdArp(SDWanNode sdWanNode, IpPacket packet) {
        String ip = packet.getDstIP();
        return CompletableFuture.supplyAsync(() -> {
            SDWanProtos.SDArpResp sdArp = sdArpCache.get(ip);
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
            sdArpCache.put(ip, sdArp);
            TIMEOUT.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    sdArpCache.remove(ip);
                }
            }, sdArp.getTtl(), TimeUnit.SECONDS);
            return sdArp;
        }).thenComposeAsync(sdArp -> {
            if (null == sdArp) {
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<InetSocketAddress> future = punchingManager.getPublicAddress(sdArp);
            return future;
        });
    }
}

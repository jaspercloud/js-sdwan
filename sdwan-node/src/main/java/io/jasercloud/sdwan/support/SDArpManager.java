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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class SDArpManager {

    public Timer TIMEOUT = new HashedWheelTimer(
            new DefaultThreadFactory("sd-arp-timeout", true),
            20, TimeUnit.MILLISECONDS);
    private Map<String, AtomicReference<SDWanProtos.SDArpResp>> sdArpCache = new ConcurrentHashMap<>();

    private PunchingManager punchingManager;

    public SDArpManager(PunchingManager punchingManager) {
        this.punchingManager = punchingManager;
    }

    @EventListener(NodeOfflineEvent.class)
    public void onNodeOfflineEvent(NodeOfflineEvent event) {
        sdArpCache.remove(event.getIp());
    }

    public CompletableFuture<InetSocketAddress> sdArp(SDWanNode sdWanNode, String localVIP, IpPacket packet) {
        String dstIP = packet.getDstIP();
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<SDWanProtos.SDArpResp> ref = sdArpCache.get(dstIP);
            return ref;
        }).thenComposeAsync(ref -> {
            if (null == ref) {
                log.info("sdArpQuery: {}", dstIP);
                return sdWanNode.sdArp(dstIP, 3000)
                        .thenApply(sdArp -> {
                            sdArpCache.put(dstIP, new AtomicReference<>(sdArp));
                            TIMEOUT.newTimeout(new TimerTask() {
                                @Override
                                public void run(Timeout timeout) throws Exception {
                                    sdArpCache.remove(dstIP);
                                }
                            }, sdArp.getTtl(), TimeUnit.SECONDS);
                            return sdArp;
                        });
            }
            return CompletableFuture.completedFuture(ref.get());
        }).thenComposeAsync(sdArp -> {
            if (SDWanProtos.MessageCode.Success_VALUE != sdArp.getCode()) {
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<InetSocketAddress> future = punchingManager.getPublicAddress(localVIP, packet, sdArp);
            return future;
        });
    }
}

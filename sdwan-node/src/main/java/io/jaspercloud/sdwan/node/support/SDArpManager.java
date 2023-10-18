package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.IpPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

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

    @EventListener(NodeOfflineEvent.class)
    public void onNodeOfflineEvent(NodeOfflineEvent event) {
        sdArpCache.remove(event.getIp());
    }

    public CompletableFuture<SDWanProtos.SDArpResp> sdArp(SDWanNode sdWanNode, IpPacket packet) {
        String dstIP = packet.getDstIP();
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<SDWanProtos.SDArpResp> ref = sdArpCache.get(dstIP);
            return ref;
        }).thenComposeAsync(ref -> {
            if (null == ref) {
                log.debug("sdArpQuery: {}", dstIP);
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
        }).thenApply(sdArp -> {
            if (SDWanProtos.MessageCode.Success_VALUE != sdArp.getCode()) {
                return null;
            }
            return sdArp;
        });
    }
}

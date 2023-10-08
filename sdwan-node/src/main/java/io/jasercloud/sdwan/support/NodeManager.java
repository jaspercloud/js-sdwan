package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.*;
import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NodeManager implements InitializingBean {

    private SDWanNode sdWanNode;
    private StunClient stunClient;

    private Map<String, NodeHeart> nodeHeartMap = new ConcurrentHashMap<>();

    public NodeManager(SDWanNode sdWanNode, StunClient stunClient) {
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread thread = new Thread(() -> {
            while (true) {
                for (Map.Entry<String, NodeHeart> entry : nodeHeartMap.entrySet()) {
                    NodeHeart nodeHeart = entry.getValue();
                    long diffTime = System.currentTimeMillis() - nodeHeart.getLastHeart();
                    if (diffTime > (30 * 1000)) {
                        stunClient.sendBind(nodeHeart.getAddress())
                                .thenAccept(packet -> {
                                    nodeHeart.setLastHeart(System.currentTimeMillis());
                                });
                    }
                }
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "node-heart");
        thread.start();
    }

    public CompletableFuture<InetSocketAddress> getPublicAddress(SDWanProtos.SDArpResp sdArpResp) {
        try {
            String vip = sdArpResp.getVip();
            String stunMapping = sdArpResp.getStunMapping();
            String stunFiltering = sdArpResp.getStunFiltering();
            CheckResult self = stunClient.getSelfCheckResult();
            InetSocketAddress address = self.getMappingAddress();
            if (StunRule.EndpointIndependent.equals(self.getFiltering())
                    && StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress resp = new InetSocketAddress(sdArpResp.getPublicIP(), sdArpResp.getPublicPort());
                return CompletableFuture.completedFuture(resp);
            } else if (StunRule.EndpointIndependent.equals(self.getFiltering())) {
                String tranId = StunMessage.genTranId();
                CompletableFuture<StunPacket> future = AsyncTask.waitTask(tranId, 3000);
                sdWanNode.punching(address.getHostString(), address.getPort(), vip, tranId);
                return future.thenApply(e -> e.recipient()).thenApply(addr -> {
                    nodeHeartMap.put(vip, new NodeHeart(addr, System.currentTimeMillis()));
                    return addr;
                });
            } else if (StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress target = new InetSocketAddress(sdArpResp.getPublicIP(), sdArpResp.getPublicPort());
                CompletableFuture<StunPacket> future = stunClient.sendBind(target);
                return future.thenApply(e -> e.recipient()).thenApply(addr -> {
                    nodeHeartMap.put(vip, new NodeHeart(addr, System.currentTimeMillis()));
                    return addr;
                });
            } else if (StunRule.AddressDependent.equals(self.getFiltering())) {

            } else if (StunRule.AddressDependent.equals(stunFiltering)) {

            }
            return null;
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    @Data
    public static class NodeHeart {

        private InetSocketAddress address;
        private Long lastHeart;

        public NodeHeart(InetSocketAddress address, Long lastHeart) {
            this.address = address;
            this.lastHeart = lastHeart;
        }
    }
}

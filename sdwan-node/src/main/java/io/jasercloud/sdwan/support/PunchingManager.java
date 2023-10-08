package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.*;
import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PunchingManager implements InitializingBean {

    @Autowired
    private ApplicationEventPublisher publisher;

    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private InetSocketAddress stunServer;

    private Map<String, Node> nodeMap = new ConcurrentHashMap<>();

    private CheckResult checkResult;

    public CheckResult getCheckResult() {
        return checkResult;
    }

    public PunchingManager(SDWanNode sdWanNode, StunClient stunClient, InetSocketAddress stunServer) {
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.stunServer = stunServer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        checkResult = stunClient.check(stunServer, 3000);
        Thread stunCheckThread = new Thread(() -> {
            while (true) {
                try {
                    checkResult = stunClient.check(stunServer, 3000);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "stun-check");
        stunCheckThread.setDaemon(true);
        stunCheckThread.start();
        Thread nodeHeartThread = new Thread(() -> {
            while (true) {
                for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
                    String vip = entry.getKey();
                    Node nodeHeart = entry.getValue();
                    stunClient.sendBind(nodeHeart.getAddress(), 3000)
                            .whenComplete((packet, throwable) -> {
                                if (null != throwable) {
                                    nodeMap.remove(vip);
                                    publisher.publishEvent(new NodeOfflineEvent(this, vip));
                                    log.error("punchingTimout: {}", nodeHeart.getAddress());
                                    return;
                                }
                                nodeHeart.setLastHeart(System.currentTimeMillis());
                            });
                }
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "node-heart");
        nodeHeartThread.setDaemon(true);
        nodeHeartThread.start();
    }

    public CompletableFuture<InetSocketAddress> getPublicAddress(SDWanProtos.SDArpResp sdArpResp) {
        try {
            String vip = sdArpResp.getVip();
            String stunMapping = sdArpResp.getStunMapping();
            String stunFiltering = sdArpResp.getStunFiltering();
            Node nodeHeart = nodeMap.get(vip);
            if (null != nodeHeart) {
                InetSocketAddress address = nodeHeart.getAddress();
                return CompletableFuture.completedFuture(address);
            }
            CheckResult self = getCheckResult();
            InetSocketAddress address = self.getMappingAddress();
            if (StunRule.EndpointIndependent.equals(self.getFiltering())
                    && StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress resp = new InetSocketAddress(sdArpResp.getPublicIP(), sdArpResp.getPublicPort());
                return CompletableFuture.completedFuture(resp);
            } else if (StunRule.EndpointIndependent.equals(self.getFiltering())) {
                String tranId = StunMessage.genTranId();
                CompletableFuture<StunPacket> future = AsyncTask.waitTask(tranId, 3000);
                sdWanNode.punching(address.getHostString(), address.getPort(), vip, tranId);
                return future.thenApply(e -> e.sender()).thenApply(addr -> {
                    nodeMap.put(vip, new Node(addr, System.currentTimeMillis()));
                    return addr;
                });
            } else if (StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress target = new InetSocketAddress(sdArpResp.getPublicIP(), sdArpResp.getPublicPort());
                CompletableFuture<StunPacket> future = stunClient.sendBind(target, 3000);
                return future.thenApply(e -> e.sender()).thenApply(addr -> {
                    nodeMap.put(vip, new Node(addr, System.currentTimeMillis()));
                    return addr;
                });
            } else if (StunRule.AddressDependent.equals(self.getFiltering())) {
                // TODO: 2023/10/8
            } else if (StunRule.AddressDependent.equals(stunFiltering)) {
                // TODO: 2023/10/8
            }
            return null;
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    @Data
    public static class Node {

        private InetSocketAddress address;
        private long lastHeart;
        private int errCount;

        public Node(InetSocketAddress address, long lastHeart) {
            this.address = address;
            this.lastHeart = lastHeart;
        }
    }
}

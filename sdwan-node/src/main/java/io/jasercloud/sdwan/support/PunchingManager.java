package io.jasercloud.sdwan.support;

import io.jasercloud.sdwan.AddressAttr;
import io.jasercloud.sdwan.AttrType;
import io.jasercloud.sdwan.CheckResult;
import io.jasercloud.sdwan.MessageType;
import io.jasercloud.sdwan.ProtoFamily;
import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.StunMessage;
import io.jasercloud.sdwan.StunPacket;
import io.jasercloud.sdwan.StunRule;
import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
                    Node node = entry.getValue();
                    stunClient.sendBind(node.getAddress(), 3000)
                            .whenComplete((packet, throwable) -> {
                                if (null != throwable) {
                                    nodeMap.remove(vip);
                                    publisher.publishEvent(new NodeOfflineEvent(this, vip));
                                    log.error("punchingTimout: {}", node.getAddress());
                                    return;
                                }
                                node.setLastHeart(System.currentTimeMillis());
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
        stunClient.getChannel().pipeline().addLast(new StunChannelInboundHandler(MessageType.BindRequest) {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                Channel channel = ctx.channel();
                InetSocketAddress sender = packet.sender();
                StunMessage request = packet.content();
                StunMessage response = new StunMessage(MessageType.BindResponse);
                response.setTranId(request.getTranId());
                AddressAttr addressAttr = new AddressAttr(ProtoFamily.IPv4, sender.getHostString(), sender.getPort());
                response.getAttrs().put(AttrType.MappedAddress, addressAttr);
                StunPacket resp = new StunPacket(response, sender);
                channel.writeAndFlush(resp);
                AsyncTask.completeTask(request.getTranId(), packet);
            }
        });
        sdWanNode.addDataHandler(new SDWanDataHandler<SDWanProtos.Punching>() {

            @Override
            public void onData(SDWanProtos.Punching request) {
                String vip = request.getSrcVIP();
                InetSocketAddress target = new InetSocketAddress(request.getSrcIP(), request.getSrcPort());
                stunClient.sendPunchingBind(target, request.getTranId(), 3000)
                        .whenComplete((packet, throwable) -> {
                            if (null != throwable) {
                                return;
                            }
                            InetSocketAddress addr = packet.sender();
                            nodeMap.put(vip, new Node(addr, System.currentTimeMillis()));
                        });
            }
        });
    }

    public CompletableFuture<InetSocketAddress> getPublicAddress(String localVIP, SDWanProtos.SDArpResp sdArp) {
        try {
            String dstVIP = sdArp.getVip();
            String stunMapping = sdArp.getStunMapping();
            String stunFiltering = sdArp.getStunFiltering();
            Node node = nodeMap.get(dstVIP);
            if (null != node) {
                InetSocketAddress address = node.getAddress();
                return CompletableFuture.completedFuture(address);
            }
            log.info("getPublicAddress: {}", dstVIP);
            CheckResult self = getCheckResult();
            InetSocketAddress address = self.getMappingAddress();
            if (StunRule.EndpointIndependent.equals(self.getFiltering())
                    && StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress resp = new InetSocketAddress(sdArp.getPublicIP(), sdArp.getPublicPort());
                return CompletableFuture.completedFuture(resp);
            } else if (StunRule.EndpointIndependent.equals(self.getFiltering())) {
                String tranId = StunMessage.genTranId();
                CompletableFuture<StunPacket> future = AsyncTask.waitTask(tranId, 3000);
                sdWanNode.forwardPunching(localVIP, dstVIP, address.getHostString(), address.getPort(), tranId);
                return future.thenApply(e -> e.sender()).thenApply(addr -> {
                    nodeMap.put(dstVIP, new Node(addr, System.currentTimeMillis()));
                    return addr;
                });
            } else if (StunRule.EndpointIndependent.equals(stunFiltering)) {
                InetSocketAddress target = new InetSocketAddress(sdArp.getPublicIP(), sdArp.getPublicPort());
                CompletableFuture<StunPacket> future = stunClient.sendBind(target, 3000);
                return future.thenApply(e -> e.sender()).thenApply(addr -> {
                    nodeMap.put(dstVIP, new Node(addr, System.currentTimeMillis()));
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

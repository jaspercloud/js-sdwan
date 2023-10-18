package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.CompletableFutures;
import io.jaspercloud.sdwan.Ecdh;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.transporter.Transporter;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tun.IpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeoutException;

@Slf4j
public class PunchingManager implements InitializingBean, Transporter.Filter {

    @Autowired
    private ApplicationEventPublisher publisher;

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private RelayClient relayClient;
    private Map<String, Node> nodeMap = new ConcurrentHashMap<>();

    private KeyPair ecdhKeyPair;
    private CheckResult checkResult;

    public CheckResult getCheckResult() {
        return checkResult;
    }

    public PunchingManager(SDWanNodeProperties properties,
                           SDWanNode sdWanNode,
                           StunClient stunClient,
                           RelayClient relayClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
        this.relayClient = relayClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ecdhKeyPair = Ecdh.generateKeyPair();
        checkResult = stunClient.check(properties.getStunServer(), 3000);
        Thread stunCheckThread = new Thread(() -> {
            while (true) {
                try {
                    checkResult = stunClient.check(properties.getStunServer(), 3000);
                } catch (TimeoutException e) {
                    log.error("checkStunServer timeout");
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof TimeoutException) {
                        log.error("checkStunServer timeout");
                    } else {
                        log.error(e.getMessage(), e);
                    }
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
                    stunClient.sendHeart(node.getAddress(), 3000)
                            .whenComplete((packet, throwable) -> {
                                if (null != throwable) {
                                    for (String accessIP : node.getAccessIPList()) {
                                        publisher.publishEvent(new NodeOfflineEvent(this, accessIP));
                                    }
                                    nodeMap.remove(vip);
                                    log.error("punchingHeartTimout: {}", node.getAddress());
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
        sdWanNode.addDataHandler(new SDWanDataHandler<SDWanProtos.Punching>() {

            //B->A forward bindReq请求
            @Override
            public void onData(ChannelHandlerContext ctx, SDWanProtos.Punching request) throws Exception {
                String vip = request.getSrcVIP();
                SDWanProtos.SocketAddress srcAddr = request.getSrcAddr();
                InetSocketAddress address = new InetSocketAddress(srcAddr.getIp(), srcAddr.getPort());
                String tranId = request.getTranId();
                StunMessage message = new StunMessage(MessageType.BindRequest, tranId);
                message.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
                message.getAttrs().put(AttrType.VIP, new StringAttr(request.getDstVIP()));
                StunPacket stunPacket = new StunPacket(message, address);
                stunClient.sendPunchingBind(stunPacket, 3000)
                        .whenComplete((packet, throwable) -> {
                            if (null != throwable) {
                                log.error("punchingBindTimout: {}", vip);
                                return;
                            }
                            //saveNode
                            try {
                                StunMessage response = packet.content();
                                StringAttr encryptKeyAttr = (StringAttr) response.getAttrs().get(AttrType.EncryptKey);
                                String publicKey = encryptKeyAttr.getData();
                                SecretKey secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(publicKey));
                                InetSocketAddress addr = packet.sender();
                                nodeMap.computeIfAbsent(vip, key -> new Node(addr, secretKey, System.currentTimeMillis()));
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            } finally {
                                packet.release();
                            }
                        });
            }
        });
        stunClient.getChannel().pipeline().addLast("bindResp", new StunChannelInboundHandler(MessageType.BindRequest) {

            //响应bindResp请求
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                Channel channel = ctx.channel();
                InetSocketAddress addr = packet.sender();
                StunMessage request = packet.content();
                //resp
                StunMessage response = new StunMessage(MessageType.BindResponse);
                response.setTranId(request.getTranId());
                AddressAttr addressAttr = new AddressAttr(ProtoFamily.IPv4, addr.getHostString(), addr.getPort());
                response.getAttrs().put(AttrType.MappedAddress, addressAttr);
                response.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
                StunPacket resp = new StunPacket(response, addr);
                channel.writeAndFlush(resp);
                //saveNode
                StringAttr encryptKeyAttr = (StringAttr) request.getAttrs().get(AttrType.EncryptKey);
                String publicKey = encryptKeyAttr.getData();
                SecretKey secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(publicKey));
                StringAttr vipAttr = (StringAttr) request.getAttrs().get(AttrType.VIP);
                String vip = vipAttr.getData();
                Node computeNode = new Node(addr, secretKey, System.currentTimeMillis());
                nodeMap.computeIfAbsent(vip, key -> computeNode);
                //completeTask
                AsyncTask.completeTask(request.getTranId(), packet);
            }
        });
    }

    public CompletableFuture<InetSocketAddress> getPublicAddress(String localVIP,
                                                                 IpPacket ipPacket,
                                                                 SDWanProtos.SDArpResp sdArp) {
        String nodeVIP = sdArp.getVip();
        Node node = nodeMap.get(nodeVIP);
        if (null != node) {
            node.addAccessIP(ipPacket.getDstIP());
            InetSocketAddress address = node.getAddress();
            return CompletableFuture.completedFuture(address);
        }
        InetSocketAddress internalAddress = new InetSocketAddress(sdArp.getInternalAddr().getIp(), sdArp.getInternalAddr().getPort());
        InetSocketAddress publicAddress = new InetSocketAddress(sdArp.getPublicAddr().getIp(), sdArp.getPublicAddr().getPort());
        CompletableFuture<StunPacket> internalFuture = punching(localVIP, sdArp, internalAddress);
        CompletableFuture<StunPacket> publicFuture = punching(localVIP, sdArp, publicAddress);
        CompletableFuture<StunPacket> future = CompletableFutures.order(internalFuture, publicFuture);
        return future.thenApply(resp -> {
            try {
                InetSocketAddress addr = resp.sender();
                Node queryNode = nodeMap.get(nodeVIP);
                queryNode.addAccessIP(ipPacket.getDstIP());
                log.debug("findPublicAddress: {} -> {}", nodeVIP, addr);
                return addr;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    private CompletableFuture<StunPacket> punching(String localVIP, SDWanProtos.SDArpResp sdArp, InetSocketAddress socketAddress) {
        String dstVIP = sdArp.getVip();
        String stunMapping = sdArp.getStunMapping();
        String stunFiltering = sdArp.getStunFiltering();
        CheckResult self = getCheckResult();
        InetSocketAddress address = self.getMappingAddress();
        if (StunRule.EndpointIndependent.equals(self.getFiltering())
                && StunRule.EndpointIndependent.equals(stunFiltering)) {
            //A -> B 发送bindReq请求
            StunMessage message = new StunMessage(MessageType.BindRequest);
            message.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
            message.getAttrs().put(AttrType.VIP, new StringAttr(localVIP));
            StunPacket request = new StunPacket(message, socketAddress);
            CompletableFuture<StunPacket> future = stunClient.sendBind(request, 3000);
            return processNodeCache(dstVIP, future);
        } else if (StunRule.EndpointIndependent.equals(self.getFiltering())) {
            String tranId = StunMessage.genTranId();
            CompletableFuture<StunPacket> future = AsyncTask.waitTask(tranId, 3000);
            sdWanNode.forwardPunching(localVIP, dstVIP, address.getHostString(), address.getPort(), tranId);
            return processNodeCache(dstVIP, future);
        } else if (StunRule.EndpointIndependent.equals(stunFiltering)) {
            //A -> B 发送bindReq请求
            StunMessage message = new StunMessage(MessageType.BindRequest);
            message.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
            message.getAttrs().put(AttrType.VIP, new StringAttr(localVIP));
            StunPacket request = new StunPacket(message, socketAddress);
            CompletableFuture<StunPacket> future = stunClient.sendBind(request, 3000);
            return processNodeCache(dstVIP, future);
        } else if (StunRule.AddressDependent.equals(self.getFiltering())) {
            // TODO: 2023/10/11 test
            //A -> B 发送bindReq请求
            StunMessage message = new StunMessage(MessageType.BindRequest);
            message.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
            StunPacket request = new StunPacket(message, socketAddress);
            CompletableFuture<StunPacket> future = stunClient.sendBind(request, 3000);
            return processNodeCache(dstVIP, future);
        } else if (StunRule.AddressDependent.equals(stunFiltering)) {
            // TODO: 2023/10/11 test
            //A -> B 发送bindReq请求
            StunMessage message = new StunMessage(MessageType.BindRequest);
            message.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
            message.getAttrs().put(AttrType.VIP, new StringAttr(localVIP));
            StunPacket request = new StunPacket(message, socketAddress);
            CompletableFuture<StunPacket> future = stunClient.sendBind(request, 3000);
            return processNodeCache(dstVIP, future);
        } else {
            try {
                //Symmetric对称网络，Relay
                StunMessage stunMessage = new StunMessage(MessageType.BindResponse);
                StunPacket packet = new StunPacket(stunMessage, properties.getRelayServer(), properties.getRelayServer());
                Node computeNode = new Node(properties.getRelayServer(), relayClient.getSecretKey(), System.currentTimeMillis());
                nodeMap.computeIfAbsent(dstVIP, key -> computeNode);
                return CompletableFuture.completedFuture(packet);
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }
    }

    private CompletableFuture<StunPacket> processNodeCache(String vip, CompletableFuture<StunPacket> future) {
        return future.thenApply(packet -> {
            try {
                StunMessage stunMessage = packet.content();
                InetSocketAddress addr = packet.sender();
                //saveNode
                StringAttr encryptKeyAttr = (StringAttr) stunMessage.getAttrs().get(AttrType.EncryptKey);
                String publicKey = encryptKeyAttr.getData();
                SecretKey secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(publicKey));
                Node computeNode = new Node(addr, secretKey, System.currentTimeMillis());
                nodeMap.computeIfAbsent(vip, key -> computeNode);
                return packet;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    @Override
    public ByteBuf encode(InetSocketAddress address, ByteBuf byteBuf) {
        Node node = nodeMap.values().stream().filter(e -> Objects.equals(e.getAddress(), address))
                .findAny().orElse(null);
        if (null == node) {
            throw new ProcessException("not found node");
        }
        try {
            byte[] bytes = Ecdh.encryptAES(ByteBufUtil.toBytes(byteBuf), node.getSecretKey());
            return ByteBufUtil.toByteBuf(bytes);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        } finally {
            byteBuf.release();
        }
    }

    @Override
    public ByteBuf decode(InetSocketAddress address, ByteBuf byteBuf) {
        Node node = nodeMap.values().stream().filter(e -> Objects.equals(e.getAddress(), address))
                .findAny().orElse(null);
        if (null == node) {
            throw new ProcessException("not found node");
        }
        try {
            byte[] bytes = Ecdh.decryptAES(ByteBufUtil.toBytes(byteBuf), node.getSecretKey());
            return ByteBufUtil.toByteBuf(bytes);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        } finally {
            byteBuf.release();
        }
    }

    @Data
    public static class Node {

        private InetSocketAddress address;
        private SecretKey secretKey;
        private Set<String> accessIPList = new ConcurrentSkipListSet<>();
        private long lastHeart;

        public void addAccessIP(String ip) {
            accessIPList.add(ip);
        }

        public Node(InetSocketAddress address, SecretKey secretKey, long lastHeart) {
            this.address = address;
            this.secretKey = secretKey;
            this.lastHeart = lastHeart;
        }
    }
}

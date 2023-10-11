package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.AddressAttr;
import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.AttrType;
import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.CheckResult;
import io.jaspercloud.sdwan.CompletableFutures;
import io.jaspercloud.sdwan.Ecdh;
import io.jaspercloud.sdwan.MessageType;
import io.jaspercloud.sdwan.ProtoFamily;
import io.jaspercloud.sdwan.StringAttr;
import io.jaspercloud.sdwan.StunClient;
import io.jaspercloud.sdwan.StunMessage;
import io.jaspercloud.sdwan.StunPacket;
import io.jaspercloud.sdwan.StunRule;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.transporter.Transporter;
import io.jaspercloud.sdwan.tun.IpPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeoutException;

@Slf4j
public class PunchingManager implements InitializingBean, Transporter.Filter {

    @Autowired
    private ApplicationEventPublisher publisher;

    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private InetSocketAddress stunServer;

    private Map<String, Node> nodeMap = new ConcurrentHashMap<>();

    private KeyPair ecdhKeyPair;
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
        ecdhKeyPair = Ecdh.generateKeyPair();
        checkResult = stunClient.check(stunServer, 3000);
        Thread stunCheckThread = new Thread(() -> {
            while (true) {
                try {
                    checkResult = stunClient.check(stunServer, 3000);
                } catch (TimeoutException e) {
                    log.error("checkStunServer timeout");
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
                    stunClient.sendHeart(node.getAddress(), 3000)
                            .whenComplete((packet, throwable) -> {
                                if (null != throwable) {
                                    for (String accessIP : node.getAccessIPList()) {
                                        publisher.publishEvent(new NodeOfflineEvent(this, accessIP));
                                    }
                                    nodeMap.remove(vip);
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
                message.getAttrs().put(AttrType.VIP, new StringAttr(vip));
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
        stunClient.getChannel().pipeline().addLast(new StunChannelInboundHandler(MessageType.BindRequest) {

            //响应bingResp请求
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                Channel channel = ctx.channel();
                InetSocketAddress addr = packet.sender();
                StunMessage request = packet.content();
                StunMessage response = new StunMessage(MessageType.BindResponse);
                response.setTranId(request.getTranId());
                AddressAttr addressAttr = new AddressAttr(ProtoFamily.IPv4, addr.getHostString(), addr.getPort());
                response.getAttrs().put(AttrType.MappedAddress, addressAttr);
                response.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
                StunPacket resp = new StunPacket(response, addr);
                channel.writeAndFlush(resp);
                AsyncTask.completeTask(request.getTranId(), packet);
                //saveNode
                StringAttr encryptKeyAttr = (StringAttr) request.getAttrs().get(AttrType.EncryptKey);
                String publicKey = encryptKeyAttr.getData();
                StringAttr vipAttr = (StringAttr) request.getAttrs().get(AttrType.VIP);
                String vip = vipAttr.getData();
                SecretKey secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(publicKey));
                nodeMap.computeIfAbsent(vip, key -> new Node(addr, secretKey, System.currentTimeMillis()));
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
        CompletableFuture<InetSocketAddress> internalFuture = punching(localVIP, sdArp, internalAddress);
        CompletableFuture<InetSocketAddress> publicFuture = punching(localVIP, sdArp, publicAddress);
        CompletableFuture<InetSocketAddress> future = CompletableFutures.order(internalFuture, publicFuture);
        return future.thenApply(address -> {
            Node computeNode = nodeMap.get(nodeVIP);
            computeNode.addAccessIP(ipPacket.getDstIP());
            log.debug("findPublicAddress: {} -> {}", nodeVIP, address);
            return address;
        });
    }

    private CompletableFuture<InetSocketAddress> punching(String localVIP, SDWanProtos.SDArpResp sdArp, InetSocketAddress socketAddress) {
        String dstVIP = sdArp.getVip();
        String stunMapping = sdArp.getStunMapping();
        String stunFiltering = sdArp.getStunFiltering();
        CheckResult self = getCheckResult();
        InetSocketAddress address = self.getMappingAddress();
        if (StunRule.EndpointIndependent.equals(self.getFiltering())
                && StunRule.EndpointIndependent.equals(stunFiltering)) {
            return CompletableFuture.completedFuture(socketAddress);
        } else if (StunRule.EndpointIndependent.equals(self.getFiltering())) {
            String tranId = StunMessage.genTranId();
            CompletableFuture<StunPacket> future = AsyncTask.waitTask(tranId, 3000);
            sdWanNode.forwardPunching(localVIP, dstVIP, address.getHostString(), address.getPort(), tranId);
            return future.thenApply(e -> e.sender());
        } else if (StunRule.EndpointIndependent.equals(stunFiltering)) {
            //A -> B 发送bindReq请求
            StunMessage message = new StunMessage(MessageType.BindRequest);
            message.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
            message.getAttrs().put(AttrType.VIP, new StringAttr(localVIP));
            StunPacket request = new StunPacket(message, socketAddress);
            CompletableFuture<StunPacket> future = stunClient.sendBind(request, 3000);
            return future.thenApply(e -> e.sender());
        } else if (StunRule.AddressDependent.equals(self.getFiltering())) {
            // TODO: 2023/10/8
        } else if (StunRule.AddressDependent.equals(stunFiltering)) {
            // TODO: 2023/10/8
        } else {
            throw new UnsupportedOperationException();
        }
        return null;
    }

    @Override
    public ByteBuf encode(InetSocketAddress address, ByteBuf byteBuf) {
        try {
            Ipv4Packet packet = Ipv4Packet.decodeMark(byteBuf);
            Node node = nodeMap.get(packet.getDstIP());
            if (null == node) {
                throw new ProcessException("not found node");
            }
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
        try {
            Ipv4Packet packet = Ipv4Packet.decodeMark(byteBuf);
            Node node = nodeMap.get(packet.getSrcIP());
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

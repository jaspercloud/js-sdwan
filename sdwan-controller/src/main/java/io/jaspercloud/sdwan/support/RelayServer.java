package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.Ecdh;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.stun.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.InitializingBean;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RelayServer implements InitializingBean {

    private SDWanRelayProperties properties;
    private Channel channel;
    private KeyPair ecdhKeyPair;
    private Map<String, RelayNode> channelMap = new ConcurrentHashMap<>();

    public Map<String, RelayNode> getNodeMap() {
        return Collections.unmodifiableMap(channelMap);
    }

    public RelayServer(SDWanRelayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ecdhKeyPair = Ecdh.generateKeyPair();
        new Thread(() -> {
            while (true) {
                Iterator<Map.Entry<String, RelayNode>> iterator = channelMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, RelayNode> next = iterator.next();
                    RelayNode node = next.getValue();
                    long diffTime = System.currentTimeMillis() - node.getLastTime();
                    if (diffTime > properties.getTimeout()) {
                        iterator.remove();
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "relay-heart-check").start();
        InetSocketAddress local = new InetSocketAddress("0.0.0.0", properties.getPort());
        Bootstrap bootstrap = new Bootstrap()
                .group(NioEventLoopFactory.BossGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("StunClient:stunEncoder", new StunEncoder());
                        pipeline.addLast("StunClient:stunDecoder", new StunDecoder());
                        pipeline.addLast("StunClient:stunProcess", new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                InetSocketAddress sender = packet.sender();
                                StunMessage request = packet.content();
                                if (MessageType.Heart.equals(request.getMessageType())) {
                                    StunPacket response = new StunPacket(request, sender);
                                    ctx.writeAndFlush(response);
                                } else if (MessageType.RelayHeart.equals(request.getMessageType())) {
                                    StringAttr vipAttr = (StringAttr) request.getAttrs().get(AttrType.VIP);
                                    String vip = vipAttr.getData();
                                    RelayNode node = channelMap.get(vip);
                                    if (null == node) {
                                        return;
                                    }
                                    node.setLastTime(System.currentTimeMillis());
                                    StunPacket response = new StunPacket(request, sender);
                                    ctx.writeAndFlush(response);
                                } else if (MessageType.BindRelayRequest.equals(request.getMessageType())) {
                                    //resp
                                    StunMessage responseMessage = new StunMessage(MessageType.BindRelayResponse, request.getTranId());
                                    responseMessage.getAttrs().put(AttrType.EncryptKey, new StringAttr(Hex.toHexString(ecdhKeyPair.getPublic().getEncoded())));
                                    StunPacket response = new StunPacket(responseMessage, sender);
                                    ctx.writeAndFlush(response);
                                    //save
                                    StringAttr vipAttr = (StringAttr) request.getAttrs().get(AttrType.VIP);
                                    String vip = vipAttr.getData();
                                    StringAttr encryptKeyAttr = (StringAttr) request.getAttrs().get(AttrType.EncryptKey);
                                    String publicKey = encryptKeyAttr.getData();
                                    RelayNode node = new RelayNode(sender, System.currentTimeMillis());
                                    SecretKey secretKey = Ecdh.generateAESKey(ecdhKeyPair.getPrivate(), Hex.decode(publicKey));
                                    node.setSecretKey(secretKey);
                                    node.setRelayAddress(sender);
                                    channelMap.put(vip, node);
                                } else if (MessageType.Transfer.equals(request.getMessageType())) {
                                    StringAttr srcVIPAttr = (StringAttr) request.getAttrs().get(AttrType.SrcVIP);
                                    StringAttr dstVIPAttr = (StringAttr) request.getAttrs().get(AttrType.DstVIP);
                                    RelayNode srcNode = channelMap.get(srcVIPAttr.getData());
                                    if (null == srcNode) {
                                        return;
                                    }
                                    RelayNode dstNode = channelMap.get(dstVIPAttr.getData());
                                    if (null == dstNode) {
                                        return;
                                    }
                                    //resp
                                    BytesAttr dataAttr = (BytesAttr) request.getAttrs().get(AttrType.Data);
                                    byte[] bytes = Ecdh.decryptAES(dataAttr.getData(), srcNode.getSecretKey());
                                    bytes = Ecdh.encryptAES(bytes, dstNode.getSecretKey());
                                    StunMessage message = new StunMessage(MessageType.Transfer);
                                    message.getAttrs().put(AttrType.Data, new BytesAttr(bytes));
                                    StunPacket response = new StunPacket(message, dstNode.getRelayAddress());
                                    ctx.writeAndFlush(response);
                                } else {
                                    ctx.fireChannelRead(packet.retain());
                                }
                            }
                        });
                    }
                });
        channel = bootstrap.bind(local).sync().channel();
    }

    @NoArgsConstructor
    @Data
    public static class RelayNode {

        private InetSocketAddress relayAddress;
        private long lastTime;
        private SecretKey secretKey;

        public RelayNode(InetSocketAddress relayAddress, long lastTime) {
            this.relayAddress = relayAddress;
            this.lastTime = lastTime;
        }
    }
}

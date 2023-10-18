package io.jaspercloud.sdwan.infra.support;

import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.infra.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.stun.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RelayServer implements InitializingBean {

    private SDWanRelayProperties properties;
    private Channel channel;
    private Map<String, Node> channelMap = new ConcurrentHashMap<>();

    public RelayServer(SDWanRelayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            while (true) {
                Iterator<Map.Entry<String, Node>> iterator = channelMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Node> next = iterator.next();
                    Node node = next.getValue();
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
                                if (MessageType.BindRelayRequest.equals(request.getMessageType())) {
                                    StringAttr vipAttr = (StringAttr) request.getAttrs().get(AttrType.VIP);
                                    String vip = vipAttr.getData();
                                    Node node = channelMap.computeIfAbsent(vip, key -> new Node(sender, System.currentTimeMillis()));
                                    node.setLastTime(System.currentTimeMillis());
                                    //resp
                                    StunMessage responseMessage = new StunMessage(MessageType.BindRelayResponse, request.getTranId());
                                    StunPacket response = new StunPacket(responseMessage, sender);
                                    ctx.writeAndFlush(response);
                                } else if (MessageType.Transfer.equals(request.getMessageType())) {
                                    StringAttr vipAttr = (StringAttr) request.getAttrs().get(AttrType.VIP);
                                    Node node = channelMap.get(vipAttr.getData());
                                    if (null == node) {
                                        return;
                                    }
                                    //resp
                                    StunPacket response = new StunPacket(request.retain(), node.getAddress());
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

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class Node {

        private InetSocketAddress address;
        private long lastTime;
    }
}

package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.stun.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RelayServer implements InitializingBean, DisposableBean {

    private SDWanRelayProperties properties;
    private Channel localChannel;
    private Map<String, RelayNode> channelMap = new ConcurrentHashMap<>();

    public Map<String, RelayNode> getNodeMap() {
        return Collections.unmodifiableMap(channelMap);
    }

    public RelayServer(SDWanRelayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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
                    Thread.sleep(5000);
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
                        pipeline.addLast("RelayServer:stunEncoder", new StunEncoder());
                        pipeline.addLast("RelayServer:stunDecoder", new StunDecoder());
                        pipeline.addLast("RelayServer:stunProcess", new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                process(ctx, packet);
                            }
                        });
                    }
                });
        localChannel = bootstrap.bind(local).syncUninterruptibly().channel();
        log.info("relayServer started: port={}", properties.getPort());
    }

    @Override
    public void destroy() throws Exception {
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }

    private void process(ChannelHandlerContext ctx, StunPacket packet) {
        StunMessage request = packet.content();
        InetSocketAddress sender = packet.sender();
        if (MessageType.BindRelayRequest.equals(request.getMessageType())) {
            processBindRelay(ctx, packet);
        } else if (MessageType.Heart.equals(request.getMessageType())) {
            processRelayHeart(ctx, packet);
        } else if (MessageType.Transfer.equals(request.getMessageType())) {
            processTransfer(ctx, packet);
        } else {
            ctx.fireChannelRead(packet.retain());
        }
    }

    private void processBindRelay(ChannelHandlerContext ctx, StunPacket packet) {
        InetSocketAddress sender = packet.sender();
        StunMessage request = packet.content();
        //parse
        StringAttr relayTokenAttr = (StringAttr) request.getAttrs().get(AttrType.RelayToken);
        String relayToken = relayTokenAttr.getData();
        channelMap.put(relayToken, new RelayNode(sender));
        //resp
        StunMessage responseMessage = new StunMessage(MessageType.BindRelayResponse, request.getTranId());
        StunPacket response = new StunPacket(responseMessage, sender);
        ctx.writeAndFlush(response);
    }

    private void processRelayHeart(ChannelHandlerContext ctx, StunPacket packet) {
        InetSocketAddress sender = packet.sender();
        StunMessage request = packet.content();
        //parse
        StringAttr relayTokenAttr = (StringAttr) request.getAttrs().get(AttrType.DstRelayToken);
        String relayToken = relayTokenAttr.getData();
        RelayNode node = channelMap.get(relayToken);
        if (null == node) {
            return;
        }
        //resp
        StunPacket response = new StunPacket(request, node.getTargetAddress());
        ctx.writeAndFlush(response);
    }

    private void processTransfer(ChannelHandlerContext ctx, StunPacket packet) {
        InetSocketAddress sender = packet.sender();
        StunMessage request = packet.content();
        //parse
        StringAttr relayTokenAttr = (StringAttr) request.getAttrs().get(AttrType.DstRelayToken);
        String relayToken = relayTokenAttr.getData();
        RelayNode node = channelMap.get(relayToken);
        if (null == node) {
            return;
        }
        //resp
        BytesAttr dataAttr = (BytesAttr) request.getAttrs().get(AttrType.Data);
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.getAttrs().put(AttrType.Data, dataAttr);
        StunPacket response = new StunPacket(message, node.getTargetAddress());
        ctx.writeAndFlush(response);
    }

    public static class RelayNode {

        private InetSocketAddress targetAddress;
        private long lastTime = System.currentTimeMillis();

        public InetSocketAddress getTargetAddress() {
            return targetAddress;
        }

        public long getLastTime() {
            return lastTime;
        }

        public RelayNode(InetSocketAddress targetAddress) {
            this.targetAddress = targetAddress;
        }
    }
}

package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import sun.net.util.IPAddressUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class StunClient implements InitializingBean {

    private int port;
    private Channel localChannel;
    private List<StunDataHandler> dataHandlerList = new ArrayList<>();

    public Channel getChannel() {
        return localChannel;
    }

    public StunClient() {
        this(0);
    }

    public StunClient(int port) {
        this.port = port;
    }

    public void addDataHandler(StunDataHandler handler) {
        dataHandlerList.add(handler);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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
                                StunMessage request = packet.content();
                                InetSocketAddress sender = packet.sender();
                                if (MessageType.BindRequest.equals(request.getMessageType())) {
                                    processBindRequest(ctx, packet);
                                } else if (MessageType.BindResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.BindRelayResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else {
                                    for (StunDataHandler handler : dataHandlerList) {
                                        handler.onData(ctx, packet);
                                    }
                                }
                            }
                        });
                    }
                });
        InetSocketAddress localAddress = new InetSocketAddress("0.0.0.0", port);
        localChannel = bootstrap.bind(localAddress).sync().channel();
    }

    private void processBindRequest(ChannelHandlerContext ctx, StunPacket request) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = request.sender();
        ProtoFamily protoFamily;
        if (IPAddressUtil.isIPv4LiteralAddress(sender.getHostString())) {
            protoFamily = ProtoFamily.IPv4;
        } else if (IPAddressUtil.isIPv6LiteralAddress(sender.getHostString())) {
            protoFamily = ProtoFamily.IPv6;
        } else {
            throw new UnsupportedOperationException();
        }
        StunMessage stunMessage = new StunMessage(MessageType.BindResponse, request.content().getTranId());
        stunMessage.setAttr(AttrType.MappedAddress, new AddressAttr(protoFamily, sender.getHostString(), sender.getPort()));
        StunPacket response = new StunPacket(stunMessage, request.sender());
        channel.writeAndFlush(response);
    }

    public CompletableFuture<StunPacket> invokeAsync(StunPacket request) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 500);
        localChannel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> invokeAsync(StunPacket request, long timeout) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        localChannel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public CompletableFuture<StunPacket> sendChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(changeIP, changePort);
        message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public void send(InetSocketAddress address, StunMessage message) {
        StunPacket request = new StunPacket(message, address);
        localChannel.writeAndFlush(request);
    }
}

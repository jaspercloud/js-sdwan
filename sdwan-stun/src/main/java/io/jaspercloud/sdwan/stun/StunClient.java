package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class StunClient implements InitializingBean {

    private Channel channel;
    private List<StunDataHandler> dataHandlerList = new ArrayList<>();

    public Channel getChannel() {
        return channel;
    }

    public void addStunDataHandler(StunDataHandler handler) {
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
                                Channel channel = ctx.channel();
                                InetSocketAddress sender = packet.sender();
                                StunMessage request = packet.content();
                                if (MessageType.Heart.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.BindResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.BindRelayResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else {
                                    for (StunDataHandler handler : dataHandlerList) {
                                        handler.receive(ctx, request);
                                    }
                                }
                            }
                        });
                    }
                });
        InetSocketAddress localAddress = new InetSocketAddress("0.0.0.0", 0);
        channel = bootstrap.bind(localAddress).sync().channel();
    }

    //    public CompletableFuture<StunPacket> sendPunchingBind(StunPacket request, long timeout) {
//        StunMessage stunMessage = request.content();
//        CompletableFuture<StunPacket> future = AsyncTask.waitTask(stunMessage.getTranId(), timeout);
//        channel.writeAndFlush(request);
//        return future;
//    }
//
//    public CompletableFuture<StunPacket> sendBind(StunPacket request, long timeout) {
//        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
//        channel.writeAndFlush(request);
//        return future;
//    }

    public StunPacket invokeSync(StunPacket request) throws Exception {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 500);
        channel.writeAndFlush(request);
        return future.get();
    }

    public CompletableFuture<StunPacket> invokeAsync(StunPacket request) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 500);
        channel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendHeart(InetSocketAddress address) {
        StunMessage message = new StunMessage(MessageType.Heart);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(changeIP, changePort);
        message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request);
        return future;
    }

    public void send(InetSocketAddress address, StunMessage message) {
        StunPacket request = new StunPacket(message, address);
        channel.writeAndFlush(request);
    }
}

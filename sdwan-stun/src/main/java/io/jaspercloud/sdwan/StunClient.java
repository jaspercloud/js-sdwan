package io.jaspercloud.sdwan;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class StunClient implements InitializingBean {

    private Channel channel;
    private InetSocketAddress local;

    public Channel getChannel() {
        return channel;
    }

    public StunClient(InetSocketAddress local) {
        this.local = local;
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
                                    StunPacket response = new StunPacket(request, sender);
                                    ctx.writeAndFlush(response);
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.BindResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else {
                                    ctx.fireChannelRead(packet.retain());
                                }
                            }
                        });
                    }
                });
        channel = bootstrap.bind(local).sync().channel();
    }

    public CheckResult check(InetSocketAddress remote, long timeout) throws Exception {
        String mapping;
        String filtering;
        StunPacket response = sendBind(remote, timeout).get();
        int localPort = response.recipient().getPort();
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr otherAddressAttr = (AddressAttr) attrs.get(AttrType.OtherAddress);
        InetSocketAddress otherAddress = new InetSocketAddress(otherAddressAttr.getIp(), otherAddressAttr.getPort());
        AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress1 = new InetSocketAddress(mappedAddressAttr.getIp(), mappedAddressAttr.getPort());
        if (Objects.equals(mappedAddress1, local)) {
            mapping = StunRule.Internet;
            filtering = StunRule.Internet;
            return new CheckResult(localPort, mapping, filtering, mappedAddress1);
        }
        if (null != (response = testChangeBind(remote, true, true, timeout))) {
            filtering = StunRule.EndpointIndependent;
        } else if (null != (response = testChangeBind(remote, false, true, timeout))) {
            filtering = StunRule.AddressDependent;
        } else {
            InetSocketAddress addr = new InetSocketAddress(otherAddress.getHostString(), remote.getPort());
            response = sendBind(addr, timeout).get();
            filtering = StunRule.AddressAndPortDependent;
        }
        attrs = response.content().getAttrs();
        mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress2 = new InetSocketAddress(mappedAddressAttr.getIp(), mappedAddressAttr.getPort());
        if (Objects.equals(mappedAddress1, mappedAddress2)) {
            mapping = StunRule.EndpointIndependent;
            return new CheckResult(localPort, mapping, filtering, mappedAddress1);
        } else if (Objects.equals(mappedAddress1.getHostString(), mappedAddress2.getHostString())) {
            mapping = StunRule.AddressDependent;
            return new CheckResult(localPort, mapping, filtering, null);
        } else {
            mapping = StunRule.AddressAndPortDependent;
            return new CheckResult(localPort, mapping, filtering, null);
        }
    }

    public CompletableFuture<StunPacket> sendPunchingBind(StunPacket request, long timeout) {
        StunMessage stunMessage = request.content();
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(stunMessage.getTranId(), timeout);
        channel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendBind(StunPacket request, long timeout) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        channel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        channel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendHeart(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.Heart);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        channel.writeAndFlush(request);
        return future;
    }

    private CompletableFuture<StunPacket> sendChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(changeIP, changePort);
        message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        channel.writeAndFlush(request);
        return future;
    }

    private StunPacket testChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort, long timeout) {
        try {
            StunPacket response = sendChangeBind(address, changeIP, changePort, timeout).get();
            return response;
        } catch (Exception e) {
            return null;
        }
    }
}

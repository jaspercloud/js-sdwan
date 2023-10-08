package io.jasercloud.sdwan;

import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class StunClient {

    private Channel channel;
    private InetSocketAddress local;

    public Channel getChannel() {
        return channel;
    }

    private StunClient(Channel channel, InetSocketAddress local) {
        this.channel = channel;
        this.local = local;
    }

    public static StunClient boot(InetSocketAddress local) throws Exception {
        Bootstrap bootstrap = new Bootstrap()
                .group(NioEventLoopFactory.BossGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new StunEncoder());
                        pipeline.addLast(new StunDecoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                Channel channel = ctx.channel();
                                InetSocketAddress sender = packet.sender();
                                StunMessage request = packet.content();
                                if (MessageType.BindRequest.equals(request.getMessageType())) {
                                    StunMessage response = new StunMessage(MessageType.BindResponse);
                                    response.setTranId(request.getTranId());
                                    AddressAttr addressAttr = new AddressAttr(ProtoFamily.IPv4, sender.getHostString(), sender.getPort());
                                    response.getAttrs().put(AttrType.MappedAddress, addressAttr);
                                    StunPacket resp = new StunPacket(response, sender);
                                    channel.writeAndFlush(resp);
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.BindResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.Transfer.equals(request.getMessageType())) {
                                    ctx.fireChannelRead(packet.retain());
                                }
                            }
                        });
                    }
                });
        Channel channel = bootstrap.bind(local).sync().channel();
        return new StunClient(channel, local);
    }

    public CheckResult check(InetSocketAddress remote, long timeout) throws Exception {
        String mapping;
        String filtering;
        StunPacket response = sendBind(remote, timeout).get();
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr otherAddressAttr = (AddressAttr) attrs.get(AttrType.OtherAddress);
        InetSocketAddress otherAddress = new InetSocketAddress(otherAddressAttr.getIp(), otherAddressAttr.getPort());
        AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress1 = new InetSocketAddress(mappedAddressAttr.getIp(), mappedAddressAttr.getPort());
        if (Objects.equals(mappedAddress1, local)) {
            mapping = StunRule.Internet;
            filtering = StunRule.Internet;
            return new CheckResult(mapping, filtering, mappedAddress1);
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
            return new CheckResult(mapping, filtering, mappedAddress1);
        } else if (Objects.equals(mappedAddress1.getHostString(), mappedAddress2.getHostString())) {
            mapping = StunRule.AddressDependent;
            return new CheckResult(mapping, filtering, null);
        } else {
            mapping = StunRule.AddressAndPortDependent;
            return new CheckResult(mapping, filtering, null);
        }
    }

    public void tryPunching(InetSocketAddress address, String tranId) {
        StunMessage message = new StunMessage(MessageType.BindRequest, tranId);
        StunPacket request = new StunPacket(message, address);
        channel.writeAndFlush(request);
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
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

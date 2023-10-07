package io.jasercloud.sdwan;

import io.jaspercloud.sdwan.AsyncTask;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StunClient {

    private InetSocketAddress local;
    private Channel channel;

    public StunClient(InetSocketAddress local) {
        this.local = local;
    }

    public CheckResult check(InetSocketAddress remote) throws Exception {
        String mapping;
        String filtering;
        StunPacket response = sendBind(remote);
        if (null == response) {
            mapping = CheckResult.Blocked;
            filtering = CheckResult.Blocked;
            return new CheckResult(mapping, filtering, null);
        }
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr otherAddressAttr = (AddressAttr) attrs.get(AttrType.OtherAddress);
        InetSocketAddress otherAddress = new InetSocketAddress(otherAddressAttr.getIp(), otherAddressAttr.getPort());
        AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress1 = new InetSocketAddress(mappedAddressAttr.getIp(), mappedAddressAttr.getPort());
        if (Objects.equals(mappedAddress1, local)) {
            mapping = CheckResult.Internet;
            filtering = CheckResult.Internet;
            return new CheckResult(mapping, filtering, mappedAddress1);
        }
        if (null != (response = sendChangeBind(remote, true, true))) {
            filtering = CheckResult.EndpointIndependent;
        } else if (null != (response = sendChangeBind(remote, false, true))) {
            filtering = CheckResult.AddressDependent;
        } else {
            response = sendBind(otherAddress);
            filtering = CheckResult.AddressAndPortDependent;
        }
        attrs = response.content().getAttrs();
        mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress mappedAddress2 = new InetSocketAddress(mappedAddressAttr.getIp(), mappedAddressAttr.getPort());
        if (Objects.equals(mappedAddress1, mappedAddress2)) {
            mapping = CheckResult.EndpointIndependent;
            return new CheckResult(mapping, filtering, mappedAddress1);
        } else if (Objects.equals(mappedAddress1.getHostString(), mappedAddress2.getHostString())) {
            mapping = CheckResult.AddressDependent;
            return new CheckResult(mapping, filtering, null);
        } else {
            mapping = CheckResult.AddressAndPortDependent;
            return new CheckResult(mapping, filtering, null);
        }
    }

    public StunPacket sendBind(InetSocketAddress address) throws Exception {
        try {
            System.out.println("sendBind: " + address);
            StunMessage message = new StunMessage(MessageType.BindRequest);
            StunPacket request = new StunPacket(message, address);
            CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 1000);
            getChannel().writeAndFlush(request);
            StunPacket response = future.get(1000, TimeUnit.MILLISECONDS);
            System.out.println("response:" + response.recipient());
            return response;
        } catch (TimeoutException e) {
            return null;
        }
    }

    public StunPacket sendChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort) throws Exception {
        try {
            System.out.println(String.format("sendChangeBind=%s, ip=%s, port=%s", address, changeIP, changePort));
            StunMessage message = new StunMessage(MessageType.BindRequest);
            ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(changeIP, changePort);
            message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
            StunPacket request = new StunPacket(message, address);
            CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 1000);
            getChannel().writeAndFlush(request);
            StunPacket response = future.get(1000, TimeUnit.MILLISECONDS);
            System.out.println("response:" + response.recipient());
            return response;
        } catch (TimeoutException e) {
            return null;
        }
    }

    private Channel getChannel() throws Exception {
        if (null == channel || !channel.isActive()) {
            channel = createChannel();
        }
        return channel;
    }

    private Channel createChannel() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new StunEncoder());
                        pipeline.addLast(new StunDecoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                StunMessage message = packet.content();
                                AsyncTask.completeTask(message.getTranId(), packet);
                            }
                        });
                    }
                });
        Channel channel = bootstrap.bind(local).sync().channel();
        return channel;
    }
}

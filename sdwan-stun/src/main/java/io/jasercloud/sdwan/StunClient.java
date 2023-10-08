package io.jasercloud.sdwan;

import io.jaspercloud.sdwan.AsyncTask;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class StunClient implements InitializingBean {

    private InetSocketAddress local;
    private InetSocketAddress stunServer;
    private Channel channel;
    private CheckResult selfCheckResult;

    public Channel getChannel() {
        return channel;
    }

    public CheckResult getSelfCheckResult() {
        return selfCheckResult;
    }

    public StunClient(InetSocketAddress local, InetSocketAddress stunServer) {
        this.local = local;
        this.stunServer = stunServer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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
                                Channel channel = ctx.channel();
                                InetSocketAddress recipient = packet.recipient();
                                StunMessage request = packet.content();
                                if (MessageType.BindRequest.equals(request.getMessageType())) {
                                    StunMessage response = new StunMessage(MessageType.BindResponse);
                                    response.setTranId(request.getTranId());
                                    AddressAttr addressAttr = new AddressAttr(ProtoFamily.IPv4, recipient.getHostString(), recipient.getPort());
                                    response.getAttrs().put(AttrType.MappedAddress, addressAttr);
                                    StunPacket resp = new StunPacket(response, recipient);
                                    channel.writeAndFlush(resp);
                                } else if (MessageType.Forward.equals(request.getMessageType())) {
                                    processForward(packet);
                                }
                                AsyncTask.completeTask(request.getTranId(), packet);
                            }
                        });
                    }
                });
        channel = bootstrap.bind(local).sync().channel();
        if (null != stunServer) {
            selfCheckResult = check(stunServer);
            System.out.println(String.format("mapping=%s, filtering=%s, address=%s",
                    selfCheckResult.getMapping(), selfCheckResult.getFiltering(), selfCheckResult.getMappingAddress()));
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        selfCheckResult = check(stunServer);
                        System.out.println(String.format("mapping=%s, filtering=%s, address=%s",
                                selfCheckResult.getMapping(), selfCheckResult.getFiltering(), selfCheckResult.getMappingAddress()));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    try {
                        Thread.sleep(30 * 1000L);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }, "stunClient");
            thread.start();
        }
    }

    protected void processForward(StunPacket packet) {

    }

    public CheckResult check(InetSocketAddress remote) throws Exception {
        String mapping;
        String filtering;
        StunPacket response = sendBind(remote).get();
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
        if (null != (response = testChangeBind(remote, true, true))) {
            filtering = StunRule.EndpointIndependent;
        } else if (null != (response = testChangeBind(remote, false, true))) {
            filtering = StunRule.AddressDependent;
        } else {
            InetSocketAddress addr = new InetSocketAddress(otherAddress.getHostString(), remote.getPort());
            response = sendBind(addr).get();
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

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 15 * 1000L);
        channel.writeAndFlush(request);
        return future;
    }

    private CompletableFuture<StunPacket> sendChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(changeIP, changePort);
        message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), 15 * 1000L);
        channel.writeAndFlush(request);
        return future;
    }

    private StunPacket testChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort) throws Exception {
        try {
            StunPacket response = sendChangeBind(address, changeIP, changePort).get(3 * 1000, TimeUnit.MILLISECONDS);
            return response;
        } catch (TimeoutException e) {
            return null;
        }
    }
}

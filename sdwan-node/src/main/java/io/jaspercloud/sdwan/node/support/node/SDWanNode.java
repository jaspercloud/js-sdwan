package io.jaspercloud.sdwan.node.support.node;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.LogHandler;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SDWanNode implements InitializingBean, DisposableBean, Runnable {

    private SDWanNodeProperties properties;
    private Bootstrap bootstrap;
    private Channel localChannel;

    private List<SDWanDataHandler> handlerList = new ArrayList<>();

    public Channel getChannel() {
        return localChannel;
    }

    public void addDataHandler(SDWanDataHandler handler) {
        handlerList.add(handler);
    }

    public SDWanNode(SDWanNodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        bootstrap = new Bootstrap();
        bootstrap.group(NioEventLoopFactory.BossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LogHandler("sdwan"));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(SDWanProtos.Message.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast("SDWanNode:heart", new ChannelInboundHandlerAdapter() {

                            private ScheduledFuture<?> heartScheduledFuture;

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                                heartScheduledFuture = ctx.executor().scheduleAtFixedRate(() -> {
                                    SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                                            .setReqId(UUID.randomUUID().toString())
                                            .setType(SDWanProtos.MsgTypeCode.HeartType)
                                            .build();
                                    ctx.channel().writeAndFlush(message);
                                }, 0, 30 * 1000, TimeUnit.MILLISECONDS);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                super.channelInactive(ctx);
                                heartScheduledFuture.cancel(true);
                            }
                        });
                        pipeline.addLast("SDWanNode:message", new SimpleChannelInboundHandler<SDWanProtos.Message>() {

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
                                switch (request.getType().getNumber()) {
                                    case SDWanProtos.MsgTypeCode.P2pOfferType_VALUE: {
                                        ctx.fireChannelRead(request);
                                        break;
                                    }
                                    case SDWanProtos.MsgTypeCode.P2pAnswerType_VALUE: {
                                        AsyncTask.completeTask(request.getReqId(), request);
                                        break;
                                    }
                                    case SDWanProtos.MsgTypeCode.RefreshRouteListType_VALUE: {
                                        ctx.fireChannelRead(request);
                                        break;
                                    }
                                    default: {
                                        AsyncTask.completeTask(request.getReqId(), request);
                                        break;
                                    }
                                }
                            }
                        });
                        pipeline.addLast("SDWanNode:dataHandler", new SimpleChannelInboundHandler<SDWanProtos.Message>() {

                            @Override
                            public void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
                                for (SDWanDataHandler handler : handlerList) {
                                    handler.onData(ctx, request);
                                }
                            }
                        });
                    }
                });
        InetSocketAddress address = properties.getControllerServer();
        localChannel = bootstrap.connect(address).syncUninterruptibly().channel();
        Thread thread = new Thread(this, "sdwan-node");
        thread.start();
    }

    @Override
    public void destroy() throws Exception {
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (null == localChannel || !localChannel.isActive()) {
                    InetSocketAddress address = properties.getControllerServer();
                    localChannel = bootstrap.connect(address).syncUninterruptibly().channel();
                }
                log.info("SDWanNode started");
                localChannel.closeFuture().sync();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public CompletableFuture<SDWanProtos.Message> invokeAsync(SDWanProtos.Message request) {
        if (!localChannel.isActive()) {
            throw new ProcessException("channel closed");
        }
        CompletableFuture<SDWanProtos.Message> future = AsyncTask.waitTask(request.getReqId(), 3000);
        localChannel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<SDWanProtos.RouteList> getRouteList() {
        SDWanProtos.Message req = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.RouteListReqType)
                .setData(ByteString.EMPTY)
                .build();
        return invokeAsync(req)
                .thenApply(resp -> {
                    try {
                        SDWanProtos.RouteList routeList = SDWanProtos.RouteList.parseFrom(resp.getData());
                        return routeList;
                    } catch (Throwable e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    public SDWanProtos.RegResp regist(List<String> addressList) throws Exception {
        InetSocketAddress localAddress = (InetSocketAddress) localChannel.localAddress();
        NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(localAddress.getHostString());
        String hardwareAddress = networkInterfaceInfo.getHardwareAddress();
        return regist(hardwareAddress, addressList);
    }

    public SDWanProtos.RegResp regist(String hardwareAddress, List<String> addressList) throws Exception {
        SDWanProtos.NodeTypeCode nodeTypeCode;
        if ("linux".equalsIgnoreCase(System.getProperty("os.name"))) {
            nodeTypeCode = SDWanProtos.NodeTypeCode.MeshType;
        } else {
            nodeTypeCode = SDWanProtos.NodeTypeCode.SimpleType;
        }
        SDWanProtos.RegReq regReq = SDWanProtos.RegReq.newBuilder()
                .setMacAddress(hardwareAddress)
                .setNodeType(nodeTypeCode)
                .addAllAddressList(addressList)
                .build();
        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.RegReqType)
                .setData(regReq.toByteString())
                .build();
        SDWanProtos.Message response = invokeAsync(request).get();
        SDWanProtos.RegResp regResp = SDWanProtos.RegResp.parseFrom(response.getData());
        return regResp;
    }

    public CompletableFuture<SDWanProtos.NodeInfoResp> queryNodeInfo(String vip) {
        SDWanProtos.NodeInfoReq nodeInfoReq = SDWanProtos.NodeInfoReq.newBuilder()
                .setVip(vip)
                .build();
        SDWanProtos.Message req = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.NodeInfoReqType)
                .setData(nodeInfoReq.toByteString())
                .build();
        return invokeAsync(req)
                .thenApply(resp -> {
                    try {
                        SDWanProtos.NodeInfoResp nodeInfo = SDWanProtos.NodeInfoResp.parseFrom(resp.getData());
                        return nodeInfo;
                    } catch (Throwable e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    public CompletableFuture<SDWanProtos.P2pAnswer> offer(String srcVIP, String dstVIP, List<String> addressList) {
        SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.newBuilder()
                .setSrcVIP(srcVIP)
                .setDstVIP(dstVIP)
                .addAllAddressList(addressList)
                .build();
        SDWanProtos.Message req = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.P2pOfferType)
                .setData(p2pOffer.toByteString())
                .build();
        return invokeAsync(req)
                .thenApply(resp -> {
                    try {
                        SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.parseFrom(resp.getData());
                        return p2pAnswer;
                    } catch (Throwable e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }
}

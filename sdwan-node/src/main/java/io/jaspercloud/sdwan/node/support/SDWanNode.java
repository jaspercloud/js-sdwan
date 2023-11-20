package io.jaspercloud.sdwan.node.support;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.*;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
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
    private Channel channel;

    private List<SDWanDataHandler> handlerList = new ArrayList<>();

    public Channel getChannel() {
        return channel;
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
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new LogHandler("sdwan"));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(SDWanProtos.Message.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast("SDWanNode:message", new SimpleChannelInboundHandler<SDWanProtos.Message>() {

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

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
                                switch (request.getType().getNumber()) {
                                    case SDWanProtos.MsgTypeCode.RefreshRouteListType_VALUE: {
                                        SDWanProtos.RouteList routeList = SDWanProtos.RouteList.parseFrom(request.getData());
                                        ctx.fireChannelRead(routeList);
                                        break;
                                    }
                                    case SDWanProtos.MsgTypeCode.PushPunchType_VALUE: {
                                        SDWanProtos.PushPunch pushPunch = SDWanProtos.PushPunch.parseFrom(request.getData());
                                        ctx.fireChannelRead(pushPunch);
                                        break;
                                    }
                                    default: {
                                        AsyncTask.completeTask(request.getReqId(), request);
                                        break;
                                    }
                                }
                            }
                        });
                        pipeline.addLast("SDWanNode:receive", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                for (SDWanDataHandler handler : handlerList) {
                                    handler.receive(ctx, msg);
                                }
                            }
                        });
                    }
                });
        InetSocketAddress address = properties.getControllerServer();
        channel = bootstrap.connect(address).syncUninterruptibly().channel();
        Thread thread = new Thread(this, "sdwan-node");
        thread.start();
    }

    @Override
    public void destroy() throws Exception {
        if (null == channel) {
            return;
        }
        channel.close();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (null == channel || !channel.isActive()) {
                    InetSocketAddress address = properties.getControllerServer();
                    channel = bootstrap.connect(address).syncUninterruptibly().channel();
                }
                log.info("SDWanNode started");
                channel.closeFuture().sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5 * 1000L);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public SDWanProtos.Message invokeSync(SDWanProtos.Message request) throws Exception {
        if (!channel.isActive()) {
            throw new ProcessException("channel closed");
        }
        CompletableFuture<SDWanProtos.Message> future = AsyncTask.waitTask(request.getReqId(), 3000);
        channel.writeAndFlush(request);
        SDWanProtos.Message response = future.get();
        return response;
    }

    public CompletableFuture<SDWanProtos.Message> invokeAsync(SDWanProtos.Message request) {
        if (!channel.isActive()) {
            throw new ProcessException("channel closed");
        }
        CompletableFuture<SDWanProtos.Message> future = AsyncTask.waitTask(request.getReqId(), 3000);
        channel.writeAndFlush(request);
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
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    public SDWanProtos.RegResp regist(MappingAddress mappingAddress, String relayToken) throws Exception {
        InetSocketAddress localAddress = (InetSocketAddress) channel.localAddress();
        NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(localAddress.getHostString());
        String hardwareAddress = networkInterfaceInfo.getHardwareAddress();
        SDWanProtos.SocketAddress internalAddr = SDWanProtos.SocketAddress.newBuilder()
                .setIp(localAddress.getHostString())
                .setPort(localAddress.getPort())
                .build();
        SDWanProtos.NodeTypeCode nodeTypeCode;
        if ("linux".equalsIgnoreCase(System.getProperty("os.name"))) {
            nodeTypeCode = SDWanProtos.NodeTypeCode.MeshType;
        } else {
            nodeTypeCode = SDWanProtos.NodeTypeCode.SimpleType;
        }
        SDWanProtos.RegReq.Builder regReqBuilder = SDWanProtos.RegReq.newBuilder()
                .setMacAddress(hardwareAddress)
                .setNodeType(nodeTypeCode)
                .setInternalAddr(internalAddr)
                .setMappingType(mappingAddress.getMappingType())
                .setRelayToken(relayToken);
        if (null != mappingAddress.getMappingAddress()) {
            SDWanProtos.SocketAddress publicAddrProto = SDWanProtos.SocketAddress.newBuilder()
                    .setIp(mappingAddress.getMappingAddress().getHostString())
                    .setPort(mappingAddress.getMappingAddress().getPort())
                    .build();
            regReqBuilder.setPublicAddr(publicAddrProto);
        }
        SDWanProtos.RegReq regReq = regReqBuilder.build();
        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.RegReqType)
                .setData(regReq.toByteString())
                .build();
        SDWanProtos.Message response = invokeSync(request);
        SDWanProtos.RegResp regResp = SDWanProtos.RegResp.parseFrom(response.getData());
        return regResp;
    }

//    public CompletableFuture<SDWanProtos.SDArpResp> sdArp(String ip) {
//        SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.newBuilder()
//                .setIp(ip)
//                .build();
//        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
//                .setReqId(UUID.randomUUID().toString())
//                .setType(SDWanProtos.MsgTypeCode.SDArpReqType)
//                .setData(nodeArpReq.toByteString())
//                .build();
//        return invokeAsync(request)
//                .thenApply(resp -> {
//                    try {
//                        SDWanProtos.SDArpResp sdArpResp = SDWanProtos.SDArpResp.parseFrom(resp.getData());
//                        return sdArpResp;
//                    } catch (Exception e) {
//                        throw new ProcessException(e.getMessage(), e);
//                    }
//                });
//    }
//
//    public CompletableFuture<InetSocketAddress> punch(InetSocketAddress srcAddr, InetSocketAddress dstAddr) {
//        SDWanProtos.PunchReq punchReq = SDWanProtos.PunchReq.newBuilder()
//                .setTranId(UUID.randomUUID().toString())
//                .setSrcAddr(SDWanProtos.SocketAddress.newBuilder()
//                        .setIp(srcAddr.getHostString())
//                        .setPort(srcAddr.getPort())
//                        .build())
//                .setDstAddr(SDWanProtos.SocketAddress.newBuilder()
//                        .setIp(dstAddr.getHostString())
//                        .setPort(dstAddr.getPort())
//                        .build())
//                .build();
//        SDWanProtos.Message req = SDWanProtos.Message.newBuilder()
//                .setReqId(UUID.randomUUID().toString())
//                .setType(SDWanProtos.MsgTypeCode.PunchReqType)
//                .setData(punchReq.toByteString())
//                .build();
//        return invokeAsync(req)
//                .thenApply(resp -> {
//                    try {
//                        SDWanProtos.PunchResp punchResp = SDWanProtos.PunchResp.parseFrom(resp.getData());
//                        if (SDWanProtos.MessageCode.Success_VALUE != punchResp.getCode()) {
//                            throw new ProcessException("punch error");
//                        }
//                        SDWanProtos.SocketAddress addr = punchResp.getAddr();
//                        InetSocketAddress socketAddress = new InetSocketAddress(addr.getIp(), addr.getPort());
//                        return socketAddress;
//                    } catch (ProcessException e) {
//                        throw e;
//                    } catch (Exception e) {
//                        throw new ProcessException(e.getMessage(), e);
//                    }
//                });
//    }

    public CompletableFuture<Boolean> checkRelayToken(String relayToken) {
        SDWanProtos.CheckRelayTokenReq tokenReq = SDWanProtos.CheckRelayTokenReq.newBuilder()
                .setToken(relayToken)
                .build();
        SDWanProtos.Message req = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.CheckRelayTokenReqType)
                .setData(tokenReq.toByteString())
                .build();
        return invokeAsync(req)
                .thenApply(resp -> {
                    try {
                        SDWanProtos.CheckRelayTokenResp checkRelayTokenResp = SDWanProtos.CheckRelayTokenResp.parseFrom(resp.getData());
                        if (SDWanProtos.MessageCode.Success_VALUE != checkRelayTokenResp.getCode()) {
                            return false;
                        }
                        return true;
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
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
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }
}

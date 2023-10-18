package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.AsyncTask;
import io.jaspercloud.sdwan.LogHandler;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.CheckResult;
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
                                    heart(ctx);
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
                                    case SDWanProtos.MsgTypeCode.RefreshRouteList_VALUE: {
                                        SDWanProtos.RouteList routeList = SDWanProtos.RouteList.parseFrom(request.getData());
                                        ctx.fireChannelRead(routeList);
                                        break;
                                    }
                                    case SDWanProtos.MsgTypeCode.PunchingType_VALUE: {
                                        SDWanProtos.Punching punchingRequest = SDWanProtos.Punching.parseFrom(request.getData());
                                        ctx.fireChannelRead(punchingRequest);
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

    public SDWanProtos.Message requestSync(SDWanProtos.Message request, int timeout) throws Exception {
        if (!channel.isActive()) {
            throw new ProcessException("channel closed");
        }
        CompletableFuture<SDWanProtos.Message> future = AsyncTask.waitTask(request.getReqId(), timeout);
        channel.writeAndFlush(request);
        SDWanProtos.Message response = future.get();
        return response;
    }

    public CompletableFuture<SDWanProtos.Message> requestAsync(SDWanProtos.Message request, int timeout) {
        if (!channel.isActive()) {
            throw new ProcessException("channel closed");
        }
        CompletableFuture<SDWanProtos.Message> future = AsyncTask.waitTask(request.getReqId(), timeout);
        channel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<SDWanProtos.SDArpResp> sdArp(String ip, int timeout) {
        SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.newBuilder()
                .setIp(ip)
                .build();
        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.SDArpReqType)
                .setData(nodeArpReq.toByteString())
                .build();
        CompletableFuture<SDWanProtos.SDArpResp> future = requestAsync(request, timeout)
                .thenApply(response -> {
                    try {
                        SDWanProtos.SDArpResp sdArpResp = SDWanProtos.SDArpResp.parseFrom(response.getData());
                        return sdArpResp;
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
        return future;
    }

    public SDWanProtos.RegResp regist(NetworkInterfaceInfo interfaceInfo,
                                      CheckResult checkResult,
                                      int timeout) throws Exception {
        String hardwareAddress = interfaceInfo.getHardwareAddress();
        SDWanProtos.SocketAddress internalAddr = SDWanProtos.SocketAddress.newBuilder()
                .setIp(interfaceInfo.getInterfaceAddress().getAddress().getHostAddress())
                .setPort(checkResult.getLocalPort())
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
                .setStunMapping(checkResult.getMapping())
                .setStunFiltering(checkResult.getFiltering());
        if (null != checkResult.getMappingAddress()) {
            InetSocketAddress mappingAddress = checkResult.getMappingAddress();
            SDWanProtos.SocketAddress publicAddr = SDWanProtos.SocketAddress.newBuilder()
                    .setIp(mappingAddress.getHostString())
                    .setPort(mappingAddress.getPort())
                    .build();
            regReqBuilder.setPublicAddr(publicAddr);
        }
        SDWanProtos.RegReq regReq = regReqBuilder.build();
        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.RegReqType)
                .setData(regReq.toByteString())
                .build();
        SDWanProtos.Message response = requestSync(request, timeout);
        SDWanProtos.RegResp regResp = SDWanProtos.RegResp.parseFrom(response.getData());
        return regResp;
    }

    private void heart(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.HeartType)
                .build();
        channel.writeAndFlush(message);
    }

    public void forwardPunching(String srcVIP, String dstVIP, String ip, int port, String tranId) {
        SDWanProtos.SocketAddress srcAddr = SDWanProtos.SocketAddress.newBuilder()
                .setIp(ip)
                .setPort(port)
                .build();
        SDWanProtos.Punching punching = SDWanProtos.Punching.newBuilder()
                .setSrcVIP(srcVIP)
                .setDstVIP(dstVIP)
                .setSrcAddr(srcAddr)
                .setTranId(tranId)
                .build();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgTypeCode.PunchingType)
                .setData(punching.toByteString())
                .build();
        channel.writeAndFlush(message);
    }
}

package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.*;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
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
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SDWanNode implements InitializingBean, DisposableBean, Runnable {

    private SDWanNodeProperties properties;
    private Bootstrap bootstrap;
    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public SDWanNode(SDWanNodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (PlatformDependent.isWindows()
                && SDWanNodeProperties.NodeType.MESH.equals(properties.getNodeType())) {
//            throw new ExceptionInInitializerError("only support linux");
        }
        bootstrap = new Bootstrap();
        bootstrap.group(NioEventLoopFactory.BossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
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
                        pipeline.addLast(new SimpleChannelInboundHandler<SDWanProtos.Message>() {

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
                                AsyncTask.completeTask(request.getReqId(), request);
                            }
                        });
                    }
                });
        InetSocketAddress address = new InetSocketAddress(properties.getControllerHost(), properties.getControllerPort());
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
                    InetSocketAddress address = new InetSocketAddress(properties.getControllerHost(), properties.getControllerPort());
                    channel = bootstrap.connect(address).syncUninterruptibly().channel();
                }
                log.info("sdwan node started");
                channel.closeFuture().sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public SDWanProtos.Message request(SDWanProtos.Message request, int timeout) throws Exception {
        channel.writeAndFlush(request);
        SDWanProtos.Message response = AsyncTask.waitSync(request.getReqId(), timeout);
        return response;
    }

    public SDWanProtos.SDArpResp sdArp(String ip, int timeout) throws Exception {
        SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.newBuilder()
                .setIp(ip)
                .build();
        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgType.NodeArpReqType)
                .setData(nodeArpReq.toByteString())
                .build();
        SDWanProtos.Message response = request(request, timeout);
        SDWanProtos.SDArpResp sdArpResp = SDWanProtos.SDArpResp.parseFrom(response.getData());
        return sdArpResp;
    }

    public SDWanProtos.RegResp regist(int timeout) throws Exception {
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(properties.getLocalIP());
        String ip = interfaceInfo.getInterfaceAddress().getAddress().getHostAddress();
        short maskBits = interfaceInfo.getInterfaceAddress().getNetworkPrefixLength();
        String hostPrefix = IPUtil.int2ip(IPUtil.ip2int(ip) >> (32 - maskBits) << (32 - maskBits));
        String hardwareAddress = interfaceInfo.getHardwareAddress();
        SDWanProtos.RegReq.Builder regReqBuilder = SDWanProtos.RegReq.newBuilder()
                .setHardwareAddress(hardwareAddress)
                .setPublicIP(properties.getStaticIP())
                .setPublicPort(properties.getStaticPort())
                .setNodeType(SDWanProtos.NodeType.forNumber(properties.getNodeType().getCode()));
        if (SDWanNodeProperties.NodeType.MESH.equals(properties.getNodeType())) {
            regReqBuilder.setCidr(String.format("%s/%s", hostPrefix, maskBits));
        }
        SDWanProtos.RegReq regReq = regReqBuilder.build();
        SDWanProtos.Message request = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgType.RegReqType)
                .setData(regReq.toByteString())
                .build();
        SDWanProtos.Message response = request(request, timeout);
        SDWanProtos.RegResp regResp = SDWanProtos.RegResp.parseFrom(response.getData());
        return regResp;
    }

    private void heart(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(UUID.randomUUID().toString())
                .setType(SDWanProtos.MsgType.HeartType)
                .build();
        channel.writeAndFlush(message);
    }
}

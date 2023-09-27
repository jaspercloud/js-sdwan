package io.jasercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jaspercloud.sdwan.LogHandler;
import io.jaspercloud.sdwan.NioEventLoopFactory;
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
import org.slf4j.impl.StaticLoggerBinder;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class SdwanClient {

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        root.setLevel(Level.INFO);
        {
            //static
            Channel channel = createChannel();
            SDWanProtos.RegReq regReq = SDWanProtos.RegReq.newBuilder()
                    .setHardwareAddress("fa:50:03:01:f8:00")
                    .setPublicIP("127.0.0.1")
                    .setPublicPort(1101)
                    .setNodeType(SDWanProtos.NodeType.MeshType)
                    .setCidr("192.222.0.0/24")
                    .build();
            SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                    .setReqId(1)
                    .setType(SDWanProtos.MsgType.RegReqType)
                    .setData(regReq.toByteString())
                    .build();
            channel.writeAndFlush(message);
        }
        {
            //dynamic
            Channel channel = createChannel();
            SDWanProtos.RegReq regReq = SDWanProtos.RegReq.newBuilder()
                    .setHardwareAddress("fa:50:03:01:f8:02")
                    .setPublicIP("127.0.0.1")
                    .setPublicPort(1102)
                    .setNodeType(SDWanProtos.NodeType.SimpleType)
                    .build();
            SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                    .setReqId(1)
                    .setType(SDWanProtos.MsgType.RegReqType)
                    .setData(regReq.toByteString())
                    .build();
            channel.writeAndFlush(message);
        }
        {
            //Heart
            Channel channel = createChannel();
            SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                    .setReqId(1)
                    .setType(SDWanProtos.MsgType.HeartType)
                    .build();
            channel.writeAndFlush(message);
        }
        Thread.sleep(3000);
        {
            //NodeArp
            Channel channel = createChannel();
            SDWanProtos.SDArpReq nodeArpReq = SDWanProtos.SDArpReq.newBuilder()
                    .setIp("192.222.0.5")
                    .build();
            SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                    .setReqId(1)
                    .setType(SDWanProtos.MsgType.NodeArpReqType)
                    .setData(nodeArpReq.toByteString())
                    .build();
            channel.writeAndFlush(message);
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    private static Channel createChannel() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NioEventLoopFactory.BossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
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
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                                switch (msg.getType().getNumber()) {
                                    case SDWanProtos.MsgType.HeartType_VALUE: {
                                        System.out.println(msg.toString());
                                        break;
                                    }
                                    case SDWanProtos.MsgType.RegRespType_VALUE: {
                                        System.out.println(msg.toString());
                                        break;
                                    }
                                    case SDWanProtos.MsgType.NodeArpRespType_VALUE: {
                                        SDWanProtos.SDArpResp sdArpResp = SDWanProtos.SDArpResp.parseFrom(msg.getData());
                                        System.out.println(msg.toString());
                                        System.out.println(sdArpResp.toString());
                                        break;
                                    }
                                }
                            }
                        });
                    }
                });
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18848);
        Channel channel = bootstrap.connect(address).syncUninterruptibly().channel();
        return channel;
    }
}

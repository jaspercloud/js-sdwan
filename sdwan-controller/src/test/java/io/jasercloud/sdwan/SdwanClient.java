package io.jasercloud.sdwan;

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

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class SdwanClient {

    public static void main(String[] args) throws Exception {
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
                                    case SDWanProtos.MsgType.RegRespType_VALUE: {
                                        System.out.println(msg.toString());
                                        break;
                                    }
                                }
                            }
                        });
                    }
                });
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 18848);
        Channel channel = bootstrap.connect(address).syncUninterruptibly().channel();
        SDWanProtos.RegReq regReq = SDWanProtos.RegReq.newBuilder()
                .setHardwareAddress("fa:50:03:01:f8:00")
                .setPublicAddress("127.0.0.1")
                .setPublicPort(8888)
                .build();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(1)
                .setType(SDWanProtos.MsgType.RegReqType)
                .setData(regReq.toByteString())
                .build();
        channel.writeAndFlush(message);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

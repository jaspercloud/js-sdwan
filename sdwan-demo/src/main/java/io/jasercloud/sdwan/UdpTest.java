package io.jasercloud.sdwan;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class UdpTest {

    public static void main(String[] args) throws Exception {
        Channel channel1 = newChannel(1001);
        Channel channel2 = newChannel(1002);
        ByteBuf buffer = channel1.alloc().buffer();
        SDWanProtos.UdpTunnelData tunnelData = SDWanProtos.UdpTunnelData.newBuilder()
                .setType(SDWanProtos.MsgType.TunnelData)
                .setSrc("10.0.0.1")
                .setDest("10.0.0.2")
                .setData(ByteString.copyFrom(new byte[1500]))
                .build();
        buffer.writeBytes(tunnelData.toByteArray());
        DatagramPacket packet = new DatagramPacket(buffer, new InetSocketAddress("127.0.0.1", 1002));
        channel1.writeAndFlush(packet);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    public static Channel newChannel(int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                                int readableBytes = msg.content().readableBytes();
                                byte[] bytes = new byte[readableBytes];
                                msg.content().readBytes(bytes);
                                SDWanProtos.UdpTunnelData tunnelData = SDWanProtos.UdpTunnelData.parseFrom(bytes);
                                System.out.println();
                            }
                        });
                    }
                });
        Channel channel = bootstrap.bind(port).syncUninterruptibly().channel();
        return channel;
    }
}

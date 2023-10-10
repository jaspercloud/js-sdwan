package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.TunAddress;
import io.jasercloud.sdwan.tun.TunChannel;
import io.jasercloud.sdwan.tun.TunChannelConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;

import java.util.concurrent.CountDownLatch;

public class TunChannelTest {

    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap = new Bootstrap()
                .group(new DefaultEventLoopGroup())
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, 1500)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
                                System.out.println();
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress("tun", "eth0"));
        TunChannel channel = (TunChannel) future.syncUninterruptibly().channel();
        channel.setAddress("192.168.1.1", 24);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

}

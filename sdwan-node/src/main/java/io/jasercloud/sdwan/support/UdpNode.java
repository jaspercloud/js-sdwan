package io.jasercloud.sdwan.support;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;

@Slf4j
public class UdpNode implements InitializingBean, DisposableBean {

    private int localPort;
    private UdpProcessHandler handler;
    private Channel channel;

    public UdpNode(int localPort, UdpProcessHandler handler) {
        this.localPort = localPort;
        this.handler = handler;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        NioEventLoopGroup loopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(loopGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                                InetSocketAddress recipient = msg.recipient();
                                log.debug("recv: localPort={}, dest={}:{}, size={}",
                                        localPort, recipient.getHostString(), recipient.getPort(), msg.content().readableBytes());
                                handler.process(ctx, msg);
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(localPort);
        future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                loopGroup.shutdownGracefully();
            }
        });
        channel = bootstrap.bind("127.0.0.1", localPort).syncUninterruptibly().channel();
    }

    @Override
    public void destroy() throws Exception {
        if (null == channel) {
            return;
        }
        channel.close();
    }

    public void write(String ip, int port, byte[] bytes) {
        if (bytes.length > 1500) {
            throw new RuntimeException();
        }
        ByteBuf buffer = channel.alloc().buffer();
        buffer.writeBytes(bytes);
        DatagramPacket packet = new DatagramPacket(buffer, new InetSocketAddress(ip, port));
        log.debug("send: localPort={}, dest={}:{}, size={}", localPort, ip, port, bytes.length);
        channel.writeAndFlush(packet);
    }

    public interface UdpProcessHandler {

        void process(ChannelHandlerContext ctx, DatagramPacket packet);
    }
}

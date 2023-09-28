package io.jasercloud.sdwan.support.transporter;

import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;

@Slf4j
public class UdpTransporter implements Transporter, InitializingBean {

    private ReceiveHandler handler;
    private Channel channel;

    @Override
    public void afterPropertiesSet() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                                if (null != handler) {
                                    Ipv4Packet ipv4Packet = Ipv4Packet.decode(msg.content());
                                    handler.onPacket(ipv4Packet);
                                }
                            }
                        });
                    }
                });
        channel = bootstrap.bind(888).sync().channel();
    }

    @Override
    public void writePacket(InetSocketAddress address, ByteBuf byteBuf) {
        DatagramPacket packet = new DatagramPacket(byteBuf, address);
        channel.writeAndFlush(packet);
    }

    @Override
    public void setReceiveHandler(ReceiveHandler handler) {
        this.handler = handler;
    }
}

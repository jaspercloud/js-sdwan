package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class TunDevice implements InitializingBean, Runnable {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private Transporter transporter;

    private TunChannel tunChannel;

    public TunDevice(SDWanNodeProperties properties, SDWanNode sdWanNode, Transporter transporter) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.transporter = transporter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        tunChannel = bootTunDevices();
        transporter.setReceiveHandler(new Transporter.ReceiveHandler() {
            @Override
            public void onPacket(IpPacket ipPacket) {

            }
        });
        Thread thread = new Thread(this, "sdwan-device");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                SDWanProtos.RegResp regResp = sdWanNode.regist(3000);
                log.info("setTunAddress: {}/{}", regResp.getVip(), regResp.getMaskBits());
                tunChannel.setAddress(regResp.getVip(), regResp.getMaskBits());
                sdWanNode.getChannel().closeFuture().sync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private TunChannel bootTunDevices() {
        Bootstrap bootstrap = new Bootstrap()
                .group(new DefaultEventLoopGroup())
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, 1500)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                Ipv4Packet ipv4Packet = Ipv4Packet.decode(msg);
                                transporter.writePacket(ipv4Packet);
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress("tun"));
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        return tunChannel;
    }
}

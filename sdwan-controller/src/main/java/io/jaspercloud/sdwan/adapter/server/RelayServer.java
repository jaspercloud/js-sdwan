package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.stun.StunDecoder;
import io.jaspercloud.sdwan.stun.StunEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;

@Slf4j
public class RelayServer implements InitializingBean, DisposableBean {

    private SDWanRelayProperties properties;
    private ChannelHandler handler;

    private Channel localChannel;

    public RelayServer(SDWanRelayProperties properties, ChannelHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public void afterPropertiesSet() {
        InetSocketAddress local = new InetSocketAddress("0.0.0.0", properties.getPort());
        Bootstrap bootstrap = new Bootstrap()
                .group(NioEventLoopFactory.BossGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("RelayServer:stunEncoder", new StunEncoder());
                        pipeline.addLast("RelayServer:stunDecoder", new StunDecoder());
                        pipeline.addLast("RelayServer:stunProcess", handler);
                    }
                });
        localChannel = bootstrap.bind(local).syncUninterruptibly().channel();
        log.info("relayServer started: port={}", properties.getPort());
    }

    @Override
    public void destroy() throws Exception {
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }

}

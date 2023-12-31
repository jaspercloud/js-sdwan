package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.LogHandler;
import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.config.SDWanControllerProperties;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class ControlServer implements InitializingBean, DisposableBean {

    private SDWanControllerProperties properties;
    private ChannelHandler handler;

    private Channel localChannel;

    public ControlServer(SDWanControllerProperties properties, ChannelHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public void afterPropertiesSet() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(NioEventLoopFactory.BossGroup, NioEventLoopFactory.WorkerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LogHandler("sdwan"));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(SDWanProtos.Message.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast("ControllerServer:process", handler);
                    }
                });
        localChannel = serverBootstrap.bind(properties.getPort()).syncUninterruptibly().channel();
        log.info("sdwan controller started: port={}", properties.getPort());
    }

    @Override
    public void destroy() throws Exception {
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }
}

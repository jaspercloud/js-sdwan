package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.NioEventLoopFactory;
import io.jaspercloud.sdwan.WinTun;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;

@Slf4j
public class SDWanNode implements InitializingBean, DisposableBean, Runnable {

    private SDWanNodeProperties properties;
    private Channel channel;

    @Autowired
    private SDWanNodeInfoManager nodeManager;

    @Autowired
    private WinTun winTun;

    @Autowired
    private UdpNode udpNode;

    public SDWanNode(SDWanNodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NioEventLoopFactory.WorkerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(SDWanProtos.SDWanMessage.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast(new NodeProcessHandler(properties, nodeManager, winTun, udpNode));
                    }
                });
        InetSocketAddress address = new InetSocketAddress(properties.getControllerHost(), properties.getControllerPort());
        channel = bootstrap.connect(address).syncUninterruptibly().channel();
        InetSocketAddress localAddr = (InetSocketAddress) channel.localAddress();
        log.info("sdwan node started: port={}", localAddr.getPort());
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
        try {
            channel.closeFuture().sync();
            InetSocketAddress localAddr = (InetSocketAddress) channel.localAddress();
            log.info("sdwan node stop: port={}", localAddr.getPort());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}

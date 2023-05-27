package io.jaspercloud.sdwan;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public interface NioEventLoopFactory {

    NioEventLoopGroup BossGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors(),
            new DefaultThreadFactory("nioBossLoop")
    );

    NioEventLoopGroup WorkerGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new DefaultThreadFactory("nioWorkerLoop")
    );
}

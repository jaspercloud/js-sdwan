package io.jaspercloud.sdwan;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class LogHandler extends ChannelInboundHandlerAdapter {

    private String type;

    public LogHandler(String type) {
        this.type = type;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        log.info("channelActive: type={}, id={}， address={}:{}",
                type, ctx.channel().id().asShortText(), address.getHostString(), address.getPort());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        log.info("channelInactive: type={}, id={}， address={}:{}",
                type, ctx.channel().id().asShortText(), address.getHostString(), address.getPort());
        super.channelInactive(ctx);
    }
}

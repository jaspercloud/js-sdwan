package io.jaspercloud.sdwan.support;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.TypeParameterMatcher;

public abstract class SDWanDataHandler<I> {

    private final TypeParameterMatcher matcher;

    public SDWanDataHandler() {
        matcher = TypeParameterMatcher.find(this, SDWanDataHandler.class, "I");
    }

    public void receive(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!matcher.match(msg)) {
            return;
        }
        onData(ctx, (I) msg);
    }

    public abstract void onData(ChannelHandlerContext ctx, I msg) throws Exception;
}

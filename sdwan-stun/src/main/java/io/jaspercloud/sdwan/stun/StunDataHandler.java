package io.jaspercloud.sdwan.stun;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.TypeParameterMatcher;

public abstract class StunDataHandler<I> {

    private final TypeParameterMatcher matcher;

    public StunDataHandler() {
        matcher = TypeParameterMatcher.find(this, StunDataHandler.class, "I");
    }

    public void receive(ChannelHandlerContext ctx, Object msg) {
        if (!matcher.match(msg)) {
            return;
        }
        onData(ctx, (I) msg);
    }

    protected abstract void onData(ChannelHandlerContext ctx, I msg);
}

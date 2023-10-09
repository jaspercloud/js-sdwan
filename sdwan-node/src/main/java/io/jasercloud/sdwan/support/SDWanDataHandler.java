package io.jasercloud.sdwan.support;

import io.netty.util.internal.TypeParameterMatcher;

public abstract class SDWanDataHandler<I> {

    private final TypeParameterMatcher matcher;

    public SDWanDataHandler() {
        matcher = TypeParameterMatcher.find(this, SDWanDataHandler.class, "I");
    }

    public void receive(Object msg) {
        if (!matcher.match(msg)) {
            return;
        }
        onData((I) msg);
    }

    public abstract void onData(I msg);
}

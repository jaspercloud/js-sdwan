package io.jaspercloud.sdwan;

import io.netty.util.ReferenceCounted;

public interface Referenced extends ReferenceCounted {

    @Override
    default ReferenceCounted touch() {
        return this;
    }

    @Override
    default ReferenceCounted touch(Object hint) {
        return this;
    }

    @Override
    default ReferenceCounted retain() {
        return retain(1);
    }

    @Override
    default boolean release() {
        return release(1);
    }
}

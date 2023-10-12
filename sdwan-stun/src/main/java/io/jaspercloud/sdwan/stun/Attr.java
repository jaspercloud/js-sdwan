package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.Referenced;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import lombok.Data;

@Data
public abstract class Attr implements Referenced {

    public abstract ByteBuf toByteBuf();

    @Override
    public ReferenceCounted retain(int increment) {
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}
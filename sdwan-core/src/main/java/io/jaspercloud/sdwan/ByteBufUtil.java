package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public class ByteBufUtil {

    private static final PooledByteBufAllocator DEFAULT = new PooledByteBufAllocator(false);

    public static ByteBuf newPacketBuf() {
        return DEFAULT.buffer(1500);
    }
}

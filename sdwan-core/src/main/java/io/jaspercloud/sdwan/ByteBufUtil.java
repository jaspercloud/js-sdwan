package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public class ByteBufUtil {

    private static final PooledByteBufAllocator DEFAULT = new PooledByteBufAllocator();

    public static ByteBuf newPacketBuf() {
        return DEFAULT.buffer(1500);
    }

    public static ByteBuf create() {
        return DEFAULT.buffer();
    }

    public static ByteBuf toByteBuf(byte[] bytes) {
        ByteBuf buffer = DEFAULT.buffer(bytes.length);
        buffer.writeBytes(bytes);
        return buffer;
    }

    public static byte[] toBytes(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public static ByteBuf heapBuffer(byte[] bytes) {
        ByteBuf byteBuf = DEFAULT.heapBuffer();
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}

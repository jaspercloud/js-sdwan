package io.jasercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Test;

public class ByteBufTest {

    public ByteBuf a(ByteBuf input) {
        input.writeByte(42);
        return input;
    }

    public ByteBuf b(ByteBuf input) {
        try {
            ByteBuf output = input.alloc().directBuffer(input.readableBytes() + 1);
            output.writeBytes(input);
            output.writeByte(42);
            return output;
        } finally {
            input.release();
        }
    }

    public void c(ByteBuf input) {
        System.out.println(input);
        input.release();
    }

    @Test
    public void test() {
        ByteBuf buf = Unpooled.directBuffer();
        c(b(a(buf)));
        int cnt = buf.refCnt();
    }
}

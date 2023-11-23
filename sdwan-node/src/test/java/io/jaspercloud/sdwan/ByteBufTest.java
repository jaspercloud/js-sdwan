package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.UUID;

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
        System.setProperty("io.netty.leakDetection.level", "ADVANCED");
        System.setProperty("io.netty.leakDetection.samplingInterval", "1");
        ByteBuf buf = Unpooled.directBuffer();
        c(b(a(buf)));
        int cnt = buf.refCnt();
        Assert.isTrue(0 == cnt);
    }
}

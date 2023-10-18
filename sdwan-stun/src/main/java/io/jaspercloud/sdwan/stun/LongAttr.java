package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LongAttr extends Attr {

    public static final Decode Decode = new Decode();

    private Long data;

    @Override
    public ByteBuf toByteBuf() {
        ByteBuf byteBuf = ByteBufUtil.create();
        byteBuf.writeLong(data);
        return byteBuf;
    }

    @Override
    public LongAttr retain(int increment) {
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            long value = byteBuf.readLong();
            return new LongAttr(value);
        }
    }
}

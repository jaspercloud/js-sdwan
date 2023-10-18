package io.jaspercloud.sdwan.stun;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ByteBufAttr extends Attr {

    public static final Decode Decode = new Decode();

    private ByteBuf byteBuf;

    @Override
    public ByteBuf toByteBuf() {
        return byteBuf;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return byteBuf.retain(increment);
    }

    @Override
    public boolean release(int decrement) {
        return byteBuf.release(decrement);
    }

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            return new ByteBufAttr(byteBuf.retain());
        }
    }
}

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

    private ByteBuf data;

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeBytes(data);
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return data.retain(increment);
    }

    @Override
    public boolean release(int decrement) {
        return data.release(decrement);
    }

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            return new ByteBufAttr(byteBuf.retain());
        }
    }
}

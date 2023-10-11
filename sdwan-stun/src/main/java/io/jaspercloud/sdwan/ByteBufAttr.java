package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ByteBufAttr extends Attr {

    private ByteBuf byteBuf;

    @Override
    public ReferenceCounted retain(int increment) {
        return byteBuf.retain(increment);
    }

    @Override
    public boolean release(int decrement) {
        return byteBuf.release(decrement);
    }
}

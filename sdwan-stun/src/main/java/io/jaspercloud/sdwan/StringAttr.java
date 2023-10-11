package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StringAttr extends Attr {

    private String data;

    @Override
    public ByteBuf toByteBuf() {
        ByteBuf byteBuf = ByteBufUtil.toByteBuf(data.getBytes());
        return byteBuf;
    }

    @Override
    public StringAttr retain(int increment) {
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}

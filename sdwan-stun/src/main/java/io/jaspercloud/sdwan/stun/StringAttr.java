package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StringAttr extends Attr {

    public static final Decode Decode = new Decode();

    private String data;

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeBytes(data.getBytes());
    }

    @Override
    public StringAttr retain(int increment) {
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            byte[] bytes = ByteBufUtil.toBytes(byteBuf);
            String text = new String(bytes);
            return new StringAttr(text);
        }
    }
}

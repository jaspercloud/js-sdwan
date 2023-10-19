package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class BytesAttr extends Attr {

    public static final Decode Decode = new Decode();

    private byte[] data;

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeBytes(data);
    }

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            return new BytesAttr(ByteBufUtil.toBytes(byteBuf));
        }
    }
}

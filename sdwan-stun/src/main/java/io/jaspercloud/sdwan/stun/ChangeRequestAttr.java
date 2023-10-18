package io.jaspercloud.sdwan.stun;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class ChangeRequestAttr extends Attr {

    public static final Decode Decode = new Decode();

    private Boolean changeIP;
    private Boolean changePort;

    public ChangeRequestAttr() {
    }

    public ChangeRequestAttr(Boolean changeIP, Boolean changePort) {
        this.changeIP = changeIP;
        this.changePort = changePort;
    }

    @Override
    public void write(ByteBuf byteBuf) {
        int flag = 0;
        if (getChangeIP()) {
            flag |= 0b100;
        }
        if (getChangePort()) {
            flag |= 0b10;
        }
        byteBuf.writeInt(flag);
    }

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            int flag = byteBuf.readInt();
            boolean changeIP = ((flag & 0b100) >> 2) == 1;
            boolean changePort = ((flag & 0b10) >> 1) == 1;
            return new ChangeRequestAttr(changeIP, changePort);
        }
    }
}
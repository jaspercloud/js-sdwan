package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class ChangeRequestAttr extends Attr {

    private Boolean changeIP;
    private Boolean changePort;

    public ChangeRequestAttr() {
    }

    public ChangeRequestAttr(Boolean changeIP, Boolean changePort) {
        this.changeIP = changeIP;
        this.changePort = changePort;
    }

    @Override
    public ByteBuf toByteBuf() {
        ByteBuf attrByteBuf = ByteBufUtil.create();
        int flag = 0;
        if (getChangeIP()) {
            flag |= 0b100;
        }
        if (getChangePort()) {
            flag |= 0b10;
        }
        attrByteBuf.writeInt(flag);
        return attrByteBuf;
    }
}
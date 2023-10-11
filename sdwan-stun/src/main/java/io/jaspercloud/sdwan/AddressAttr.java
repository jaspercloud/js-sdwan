package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class AddressAttr extends Attr {

    private ProtoFamily family;
    private String ip;
    private Integer port;

    public AddressAttr() {
    }

    public AddressAttr(ProtoFamily family, String ip, Integer port) {
        this.family = family;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public ByteBuf toByteBuf() {
        ByteBuf attrByteBuf = ByteBufUtil.create();
        attrByteBuf.writeByte(0);
        attrByteBuf.writeByte(getFamily().getCode());
        attrByteBuf.writeShort(getPort());
        attrByteBuf.writeBytes(IPUtil.ip2bytes(getIp()));
        return attrByteBuf;
    }
}
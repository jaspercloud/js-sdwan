package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.IPUtil;
import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class AddressAttr extends Attr {

    public static final Decode Decode = new Decode();

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

    private static class Decode implements AttrDecode {

        @Override
        public Attr decode(ByteBuf byteBuf) {
            int reserved = byteBuf.readUnsignedByte();
            int family = byteBuf.readUnsignedByte();
            int port = byteBuf.readUnsignedShort();
            byte[] bytes = new byte[4];
            byteBuf.readBytes(bytes);
            String ip = IPUtil.bytes2ip(bytes);
            return new AddressAttr(ProtoFamily.valueOf(family), ip, port);
        }
    }
}
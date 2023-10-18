package io.jaspercloud.sdwan.stun;

public enum AttrType {

    MappedAddress(0x0001, AddressAttr.Decode),
    ChangeRequest(0x0003, ChangeRequestAttr.Decode),
    ResponseOrigin(0x802b, AddressAttr.Decode),
    OtherAddress(0x802c, AddressAttr.Decode),
    XorMappedAddress(0x0020, AddressAttr.Decode),
    EncryptKey(0x8001, StringAttr.Decode),
    VIP(0x8002, StringAttr.Decode),
    Data(0x8003, ByteBufAttr.Decode),
    ChannelId(0x8004, StringAttr.Decode),
    LiveTime(0x8005, LongAttr.Decode);

    private int code;
    private AttrDecode decode;

    public int getCode() {
        return code;
    }

    public AttrDecode getDecode() {
        return decode;
    }

    AttrType(int code, AttrDecode decode) {
        this.code = code;
        this.decode = decode;
    }

    public static AttrType valueOf(int code) {
        for (AttrType attrType : values()) {
            if (attrType.code == code) {
                return attrType;
            }
        }
        return null;
    }
}
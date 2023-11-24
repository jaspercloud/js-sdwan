package io.jaspercloud.sdwan.stun;

public enum AttrType {

    MappedAddress(0x0001, AddressAttr.Decode),
    SourceAddress(0x0004, AddressAttr.Decode),
    ChangedAddress(0x0005, AddressAttr.Decode),
    XorMappedAddress(0x0020, AddressAttr.Decode),
    ChangeRequest(0x0003, ChangeRequestAttr.Decode),

    //xiaomi
    ResponseOrigin(0x802b, AddressAttr.Decode),
    OtherAddress(0x802c, AddressAttr.Decode, 0x0005),

    Data(0x8003, BytesAttr.Decode),
    RelayToken(0x8004, StringAttr.Decode);

    private int code;
    private AttrDecode decode;
    private int compatibleCode;

    public int getCode() {
        return code;
    }

    public AttrDecode getDecode() {
        return decode;
    }

    public int getCompatibleCode() {
        return compatibleCode;
    }

    AttrType(int code, AttrDecode decode) {
        this.code = code;
        this.decode = decode;
    }

    AttrType(int code, AttrDecode decode, int compatibleCode) {
        this.code = code;
        this.decode = decode;
        this.compatibleCode = compatibleCode;
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
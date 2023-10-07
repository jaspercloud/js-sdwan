package io.jasercloud.sdwan;

public enum AttrType {

    MappedAddress(0x0001),
    ChangeRequest(0x0003),
    ResponseOrigin(0x802b),
    OtherAddress(0x802c),
    XorMappedAddress(0x0020);

    private int code;

    public int getCode() {
        return code;
    }

    AttrType(int code) {
        this.code = code;
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
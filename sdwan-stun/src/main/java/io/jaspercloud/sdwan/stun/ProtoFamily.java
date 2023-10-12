package io.jaspercloud.sdwan.stun;

public enum ProtoFamily {

    IPv4(1),
    IPv6(2);
    private int code;

    public int getCode() {
        return code;
    }

    ProtoFamily(int code) {
        this.code = code;
    }

    public static ProtoFamily valueOf(int code) {
        for (ProtoFamily protoFamily : values()) {
            if (protoFamily.code == code) {
                return protoFamily;
            }
        }
        return null;
    }
}
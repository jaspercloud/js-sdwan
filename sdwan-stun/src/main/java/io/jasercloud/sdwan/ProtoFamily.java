package io.jasercloud.sdwan;

public enum ProtoFamily {

    IPv4(1),
    IPv6(2);
    private int code;

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
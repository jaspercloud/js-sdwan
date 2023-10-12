package io.jaspercloud.sdwan.infra.support;

public enum IpType {

    Static(1),
    Dynamic(2);

    private int code;

    public int getCode() {
        return code;
    }

    IpType(int code) {
        this.code = code;
    }

    public static IpType valueOf(int code) {
        for (IpType ipType : values()) {
            if (ipType.getCode() == code) {
                return ipType;
            }
        }
        return null;
    }
}

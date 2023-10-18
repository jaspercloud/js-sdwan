package io.jaspercloud.sdwan.stun;

public enum MessageType {

    BindRequest(0x0001),
    BindResponse(0x0101),
    Transfer(0x0301),
    Heart(0x0302),
    BindRelayRequest(0x0303),
    BindRelayResponse(0x0343);
    private int code;

    public int getCode() {
        return code;
    }

    MessageType(int code) {
        this.code = code;
    }

    public static MessageType valueOf(int code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}

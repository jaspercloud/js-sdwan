package io.jasercloud.sdwan;

public enum MessageType {

    BindRequest(0x0001),
    BindResponse(0x0101);
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
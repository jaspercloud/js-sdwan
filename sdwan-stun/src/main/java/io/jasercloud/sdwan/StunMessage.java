package io.jasercloud.sdwan;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class StunMessage {

    public static final byte[] Cookie = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xa4, (byte) 0x42};

    private MessageType messageType;
    private String tranId = UUID.randomUUID().toString()
            .replaceAll("\\-", "")
            .substring(0, 12);
    private Map<AttrType, Attr> attrs = new HashMap<>();

    public StunMessage() {
    }

    public StunMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    public StunMessage(MessageType messageType, String tranId) {
        this.messageType = messageType;
        this.tranId = tranId;
    }
}

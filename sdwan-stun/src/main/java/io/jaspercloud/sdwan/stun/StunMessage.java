package io.jaspercloud.sdwan.stun;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class StunMessage {

    public static final byte[] Cookie = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xa4, (byte) 0x42};

    private MessageType messageType;
    private String tranId;
    private Map<AttrType, Attr> attrs = new HashMap<>();

    public <T extends Attr> T getAttr(AttrType type) {
        return (T) attrs.get(type);
    }

    public void setAttr(AttrType type, Attr attr) {
        attrs.put(type, attr);
    }

    public StunMessage() {
    }

    public StunMessage(MessageType messageType) {
        this.messageType = messageType;
        this.tranId = genTranId();
    }

    public StunMessage(MessageType messageType, String tranId) {
        if (tranId.length() != 12) {
            throw new IllegalArgumentException("tranId 12 bytes");
        }
        this.messageType = messageType;
        this.tranId = tranId;
    }

    public static String genTranId() {
        String id = UUID.randomUUID().toString()
                .replaceAll("\\-", "")
                .substring(0, 12);
        return id;
    }
}

package io.jaspercloud.sdwan.infra.support;

public enum NodeType {

    Simple(1),
    Mesh(2);

    private int code;

    public int getCode() {
        return code;
    }

    NodeType(int code) {
        this.code = code;
    }

    public static NodeType valueOf(int code) {
        for (NodeType nodeType : values()) {
            if (nodeType.getCode() == code) {
                return nodeType;
            }
        }
        return null;
    }
}

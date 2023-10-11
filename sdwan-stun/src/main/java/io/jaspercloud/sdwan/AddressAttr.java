package io.jaspercloud.sdwan;

import lombok.Data;

@Data
public class AddressAttr extends Attr {

    private ProtoFamily family;
    private String ip;
    private Integer port;

    public AddressAttr() {
    }

    public AddressAttr(ProtoFamily family, String ip, Integer port) {
        this.family = family;
        this.ip = ip;
        this.port = port;
    }
}
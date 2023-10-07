package io.jasercloud.sdwan;

import lombok.Data;

@Data
public class ChangeRequestAttr extends Attr {

    private Boolean changeIP;
    private Boolean changePort;

    public ChangeRequestAttr() {
    }

    public ChangeRequestAttr(Boolean changeIP, Boolean changePort) {
        this.changeIP = changeIP;
        this.changePort = changePort;
    }
}
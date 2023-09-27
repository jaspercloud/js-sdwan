package io.jaspercloud.sdwan.tun;

import java.net.SocketAddress;

public class TunAddress extends SocketAddress {

    private String ifName;

    public String getIfName() {
        return ifName;
    }

    public TunAddress(String ifName) {
        this.ifName = ifName;
    }
}

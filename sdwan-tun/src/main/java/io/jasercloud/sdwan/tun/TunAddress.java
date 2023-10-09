package io.jasercloud.sdwan.tun;

import java.net.SocketAddress;

public class TunAddress extends SocketAddress {

    private String ifName;
    private String vip;

    public String getIfName() {
        return ifName;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public TunAddress(String ifName) {
        this.ifName = ifName;
    }
}

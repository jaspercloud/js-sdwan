package io.jaspercloud.sdwan.tun;

import java.net.SocketAddress;

public class TunAddress extends SocketAddress {

    private String ifName;
    private String addr;
    private int netmaskPrefix;

    public String getIfName() {
        return ifName;
    }

    public String getAddr() {
        return addr;
    }

    public int getNetmaskPrefix() {
        return netmaskPrefix;
    }

    public TunAddress(String ifName, String addr, int netmaskPrefix) {
        this.ifName = ifName;
        this.addr = addr;
        this.netmaskPrefix = netmaskPrefix;
    }
}

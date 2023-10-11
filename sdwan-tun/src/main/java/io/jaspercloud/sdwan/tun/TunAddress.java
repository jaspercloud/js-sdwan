package io.jaspercloud.sdwan.tun;

import java.net.SocketAddress;

public class TunAddress extends SocketAddress {

    private String tunName;
    private String ethName;
    private String vip;

    public String getTunName() {
        return tunName;
    }

    public String getEthName() {
        return ethName;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public TunAddress(String tunName, String ethName) {
        this.tunName = tunName;
        this.ethName = ethName;
    }
}

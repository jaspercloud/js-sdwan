package io.jaspercloud.sdwan.tun;

import java.net.SocketAddress;

public class TunAddress extends SocketAddress {

    private String tunName;
    private String ethName;
    private String vip;
    private int maskBits;

    public String getTunName() {
        return tunName;
    }

    public String getEthName() {
        return ethName;
    }

    public int getMaskBits() {
        return maskBits;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public void setMaskBits(int maskBits) {
        this.maskBits = maskBits;
    }

    public TunAddress(String tunName, String ethName) {
        this.tunName = tunName;
        this.ethName = ethName;
    }
}

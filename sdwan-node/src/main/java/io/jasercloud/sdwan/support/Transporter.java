package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.tun.IpPacket;

public interface Transporter {

    void writePacket(IpPacket ipPacket);

    void setReceiveHandler(ReceiveHandler handler);

    interface ReceiveHandler {

        void onPacket(IpPacket ipPacket);
    }
}

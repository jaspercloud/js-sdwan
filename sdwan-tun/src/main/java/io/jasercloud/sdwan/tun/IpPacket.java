package io.jasercloud.sdwan.tun;

import io.jaspercloud.sdwan.Referenced;

public interface IpPacket extends Referenced {

    short getVersion();

    String getSrcIP();

    String getDstIP();
}

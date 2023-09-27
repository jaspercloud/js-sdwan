package io.jaspercloud.sdwan.tun;

import java.net.InetAddress;

public interface IpPacket {

    short getVersion();

    InetAddress getSrcIP();

    InetAddress getDstIP();
}

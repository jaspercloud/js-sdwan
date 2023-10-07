package io.jasercloud.sdwan.tun;

public interface IpPacket {

    short getVersion();

    String getSrcIP();

    String getDstIP();
}

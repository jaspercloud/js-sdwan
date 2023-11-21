package io.jaspercloud.sdwan.node.support.detection;

public interface AddressType {

    //本地网卡地址
    String HOST = "host";
    //STUN服务器获得的ip和端口生成
    String SRFLX = "srflx";
    //对端的ip和端口生成
    String PRFLX = "prflx";
    //TURN服务器获得
    String RELAY = "relay";
}

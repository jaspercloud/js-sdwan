package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.Cidr;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class NodeInfo {

    private NodeType nodeType;
    private InetSocketAddress publicAddress;
    private String macAddress;
    private String vip;
    private Cidr meshCidr;
}

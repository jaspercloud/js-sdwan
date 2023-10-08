package io.jasercloud.sdwan.support;

import io.jaspercloud.sdwan.Cidr;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Data
public class NodeInfo {

    private NodeType nodeType;
    private String stunMapping;
    private String stunFiltering;
    private InetSocketAddress publicAddress;
    private String macAddress;
    private String vip;
    private List<Cidr> routeList = new ArrayList<>();
}

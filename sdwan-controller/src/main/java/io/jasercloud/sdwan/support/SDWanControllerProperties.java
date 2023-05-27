package io.jasercloud.sdwan.support;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("sdwan.controller")
public class SDWanControllerProperties {

    private int port;
    private int netmaskPrefix;
    private Map<String, String> nodeIpMap = new HashMap<>();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNetmaskPrefix() {
        return netmaskPrefix;
    }

    public void setNetmaskPrefix(int netmaskPrefix) {
        this.netmaskPrefix = netmaskPrefix;
    }

    public Map<String, String> getNodeIpMap() {
        return nodeIpMap;
    }

    public void setNodeIpMap(Map<String, String> nodeIpMap) {
        this.nodeIpMap = nodeIpMap;
    }
}

package io.jasercloud.sdwan.support;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sdwan.node")
public class SDWanNodeProperties {

    private String controllerHost;
    private int controllerPort;
    private int connectTimeout;
    private String nodeId;
    private int nodeUdpPort;
    private String vip;

    public String getControllerHost() {
        return controllerHost;
    }

    public void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }

    public int getControllerPort() {
        return controllerPort;
    }

    public void setControllerPort(int controllerPort) {
        this.controllerPort = controllerPort;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeUdpPort() {
        return nodeUdpPort;
    }

    public void setNodeUdpPort(int nodeUdpPort) {
        this.nodeUdpPort = nodeUdpPort;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }
}

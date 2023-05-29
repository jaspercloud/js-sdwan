package io.jasercloud.sdwan.support;

public class SDWanNodeInfo {

    private String nodeId;
    private String nodeIP;
    private String vip;
    private Integer nodeUdpPort;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeIP() {
        return nodeIP;
    }

    public void setNodeIP(String nodeIP) {
        this.nodeIP = nodeIP;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public Integer getNodeUdpPort() {
        return nodeUdpPort;
    }

    public void setNodeUdpPort(Integer nodeUdpPort) {
        this.nodeUdpPort = nodeUdpPort;
    }
}

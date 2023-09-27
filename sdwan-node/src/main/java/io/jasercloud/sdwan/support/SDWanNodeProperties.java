package io.jasercloud.sdwan.support;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("sdwan.node")
public class SDWanNodeProperties {

    public enum IpType {

        STATIC("static"),
        DYNAMIC("dynamic");

        private String type;

        IpType(String type) {
            this.type = type;
        }
    }

    public enum NodeType {

        SIMPLE("simple", 1),
        MESH("mesh", 2);

        private String nodeType;
        private int code;

        public int getCode() {
            return code;
        }

        NodeType(String nodeType, int code) {
            this.nodeType = nodeType;
            this.code = code;
        }
    }

    private String controllerHost;
    private Integer controllerPort;
    private Integer connectTimeout;
    private Integer mtu;

    private IpType addressType;
    private String localIP;
    private String staticIP;
    private Integer staticPort;

    public NodeType nodeType;

    private List<String> routes;

    public void setAddressType(String ipType) {
        this.addressType = IpType.valueOf(ipType.toUpperCase());
    }

    public void setNodeType(String nodeType) {
        this.nodeType = NodeType.valueOf(nodeType.toUpperCase());
    }
}

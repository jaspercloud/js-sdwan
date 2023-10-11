package io.jaspercloud.sdwan.support;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("sdwan.node")
public class SDWanNodeProperties {

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
    public NodeType nodeType;
    private String localIP;

    public void setNodeType(String nodeType) {
        this.nodeType = NodeType.valueOf(nodeType.toUpperCase());
    }
}

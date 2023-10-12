package io.jaspercloud.sdwan.node.support;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("sdwan.node")
public class SDWanNodeProperties {

    private String controllerHost;
    private Integer controllerPort;
    private Integer connectTimeout;
    private Integer mtu;
    private String stunServer;
    private String localIP;

}

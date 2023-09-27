package io.jasercloud.sdwan.support;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    private String controllerHost;
    private Integer controllerPort;
    private Integer connectTimeout;
    private Integer mtu;
    private String localIP;
    private IpType addressType;
    private String staticIP;
    private Integer staticPort;

    public void setAddressType(String ipType) {
        this.addressType = IpType.valueOf(ipType.toUpperCase());
    }
}

package io.jasercloud.sdwan.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties("sdwan.controller")
public class SDWanControllerProperties {

    private Integer port;
    private String cidr;
    private Map<String, Node> staticNodes;

    @Data
    public static class Node {

        private String id;
        private String hardwareAddress;
        private String vip;
    }
}

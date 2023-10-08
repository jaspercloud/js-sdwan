package io.jasercloud.sdwan.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties("sdwan.controller")
public class SDWanControllerProperties {

    private Integer port;
    private String cidr;
    private Integer sdArpTTL;
    private Map<String, StaticNode> staticNodes;
    private Map<String, StaticRoute> staticRoutes;

    @Data
    public static class StaticNode {

        private String id;
        private String macAddress;
        private String vip;
    }

    @Data
    public static class StaticRoute {

        private String id;
        private String destination;
        private String nextHop;
    }
}

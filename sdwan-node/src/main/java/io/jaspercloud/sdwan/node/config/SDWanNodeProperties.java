package io.jaspercloud.sdwan.node.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetSocketAddress;

@Data
@ConfigurationProperties("sdwan.node")
public class SDWanNodeProperties {

    private Tun tun;
    private Controller controller;
    private Stun stun;
    private Relay relay;

    @Data
    public static class Tun {

        private Integer mtu;
    }

    @Data
    public static class Controller {

        private InetSocketAddress address;
        private Integer connectTimeout;
        private Long callTimeout;

        public void setAddress(String address) {
            this.address = parseAddress(address);
        }
    }

    @Data
    public static class Stun {

        private InetSocketAddress address;
        private Long callTimeout;
        private Long mappingTimeout;
        private Long heartTimeout;

        public void setAddress(String address) {
            this.address = parseAddress(address);
        }
    }

    @Data
    public static class Relay {

        private InetSocketAddress address;
        private Long heartTimeout;

        public void setAddress(String address) {
            this.address = parseAddress(address);
        }
    }

    public static InetSocketAddress parseAddress(String address) {
        String[] split = address.split("\\:");
        InetSocketAddress socketAddress = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        return socketAddress;
    }
}

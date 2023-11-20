package io.jaspercloud.sdwan.node.support;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetSocketAddress;

@Data
@ConfigurationProperties("sdwan.node")
public class SDWanNodeProperties {

    private InetSocketAddress controllerServer;
    private Integer connectTimeout;
    private Integer mtu;
    private InetSocketAddress stunServer;
    private InetSocketAddress relayServer;

    public void setControllerServer(String controllerServer) {
        this.controllerServer = parseAddress(controllerServer);
    }

    public void setStunServer(String stunServer) {
        this.stunServer = parseAddress(stunServer);
    }

    public void setRelayServer(String relayServer) {
        this.relayServer = parseAddress(relayServer);
    }

    public InetSocketAddress parseAddress(String address) {
        String[] split = address.split("\\:");
        InetSocketAddress socketAddress = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        return socketAddress;
    }
}

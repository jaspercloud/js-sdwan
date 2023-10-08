package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.support.*;
import io.jasercloud.sdwan.support.transporter.UdpTransporter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public StunClient stunClient() {
        InetSocketAddress target = new InetSocketAddress("stun.miwifi.com", 3478);
        return new StunClient(new InetSocketAddress("0.0.0.0", 0), target);
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties, StunClient stunClient) throws Exception {
        return new SDWanNode(properties, stunClient);
    }

    @Bean
    public UdpTransporter udpTransporter() {
        return new UdpTransporter();
    }

    @Bean
    public NodeManager nodeManager(SDWanNode sdWanNode, StunClient stunClient) {
        return new NodeManager(sdWanNode, stunClient);
    }

    @Bean
    public NatManager natManager(NodeManager nodeManager) {
        return new NatManager(nodeManager);
    }

    @Bean
    public TunEngine tunEngine(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               UdpTransporter udpTransporter,
                               NatManager natManager,
                               StunClient stunClient) {
        return new TunEngine(properties, sdWanNode, udpTransporter, natManager, stunClient);
    }
}

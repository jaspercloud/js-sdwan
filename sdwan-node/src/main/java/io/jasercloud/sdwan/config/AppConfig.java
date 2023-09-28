package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.NatManager;
import io.jasercloud.sdwan.support.SDWanNode;
import io.jasercloud.sdwan.support.SDWanNodeProperties;
import io.jasercloud.sdwan.support.TunDevice;
import io.jasercloud.sdwan.support.transporter.UdpTransporter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties) throws Exception {
        return new SDWanNode(properties);
    }

    @Bean
    public UdpTransporter udpTransporter() {
        return new UdpTransporter();
    }

    @Bean
    public NatManager natManager() {
        return new NatManager();
    }

    @Bean
    public TunDevice tunDevice(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               UdpTransporter udpTransporter,
                               NatManager natManager) {
        return new TunDevice(properties, sdWanNode, udpTransporter, natManager);
    }
}

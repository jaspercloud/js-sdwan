package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.*;
import io.jasercloud.sdwan.support.transporter.Transporter;
import io.jasercloud.sdwan.support.transporter.UdpTransporter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDwanNodeProcessHandler nodeProcessHandler(SDWanNodeProperties properties) {
        return new SDwanNodeProcessHandler(properties);
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties,
                               SDwanNodeProcessHandler nodeProcessHandler) {
        return new SDWanNode(properties, nodeProcessHandler);
    }

    @Bean
    public UdpTransporter udpTransporter(SDWanNode sdWanNode) {
        return new UdpTransporter(sdWanNode);
    }

    @Bean
    public TunDevice tunDevice(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               Transporter transporter) {
        return new TunDevice(properties, sdWanNode, transporter);
    }
}

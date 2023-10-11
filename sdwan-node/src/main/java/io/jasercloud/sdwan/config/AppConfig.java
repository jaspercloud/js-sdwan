package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.support.PunchingManager;
import io.jasercloud.sdwan.support.SDArpManager;
import io.jasercloud.sdwan.support.SDWanNode;
import io.jasercloud.sdwan.support.SDWanNodeProperties;
import io.jasercloud.sdwan.support.TunEngine;
import io.jasercloud.sdwan.support.transporter.StunTransporter;
import io.jasercloud.sdwan.support.transporter.Transporter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.List;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public StunClient stunClient() throws Exception {
        return new StunClient(new InetSocketAddress("0.0.0.0", 0));
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties) {
        return new SDWanNode(properties);
    }

    @Bean
    public PunchingManager punchingManager(SDWanNode sdWanNode, StunClient stunClient) {
        InetSocketAddress target = new InetSocketAddress("stun.miwifi.com", 3478);
        return new PunchingManager(sdWanNode, stunClient, target);
    }

    @Bean
    public SDArpManager sdArpManager(PunchingManager punchingManager) {
        return new SDArpManager(punchingManager);
    }

    @Bean
    public StunTransporter stunTransporter(StunClient stunClient, ObjectProvider<List<Transporter.Filter>> provider) {
        return new StunTransporter(stunClient, provider.getIfAvailable());
    }

    @Bean
    public TunEngine tunEngine(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               Transporter transporter,
                               PunchingManager punchingManager,
                               SDArpManager sdArpManager) {
        return new TunEngine(properties, sdWanNode, transporter, punchingManager, sdArpManager);
    }
}

package io.jaspercloud.sdwan.node.config;

import io.jaspercloud.sdwan.node.support.*;
import io.jaspercloud.sdwan.node.support.transporter.StunTransporter;
import io.jaspercloud.sdwan.node.support.transporter.Transporter;
import io.jaspercloud.sdwan.stun.StunClient;
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
    public RelayClient relayClient(SDWanNodeProperties properties, StunClient stunClient) {
        return new RelayClient(properties, stunClient);
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties) {
        return new SDWanNode(properties);
    }

    @Bean
    public PunchingManager punchingManager(SDWanNodeProperties properties,
                                           SDWanNode sdWanNode,
                                           StunClient stunClient,
                                           RelayClient relayClient) {
        return new PunchingManager(properties, sdWanNode, stunClient, relayClient);
    }

    @Bean
    public SDArpManager sdArpManager() {
        return new SDArpManager();
    }

    @Bean
    public StunTransporter stunTransporter(StunClient stunClient,
                                           ObjectProvider<List<Transporter.Filter>> provider) {
        return new StunTransporter(stunClient, provider.getIfAvailable());
    }

    @Bean
    public TunEngine tunEngine(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               Transporter transporter,
                               PunchingManager punchingManager,
                               RelayClient relayClient,
                               SDArpManager sdArpManager) {
        return new TunEngine(properties, sdWanNode, transporter, punchingManager, relayClient, sdArpManager);
    }
}

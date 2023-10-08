package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.StunClient;
import io.jasercloud.sdwan.support.*;
import io.jasercloud.sdwan.support.transporter.StunTransporter;
import io.jasercloud.sdwan.support.transporter.Transporter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public StunClient stunClient() throws Exception {
        return StunClient.boot(new InetSocketAddress("0.0.0.0", 0));
    }

    @Bean
    public StunTransporter stunTransporter(StunClient stunClient) {
        return new StunTransporter(stunClient);
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties, StunClient stunClient) throws Exception {
        return new SDWanNode(properties, stunClient);
    }

    @Bean
    public PunchingManager punchingManager(SDWanNode sdWanNode, StunClient stunClient) {
        InetSocketAddress target = new InetSocketAddress("stun.miwifi.com", 3478);
        return new PunchingManager(sdWanNode, stunClient, target);
    }

    @Bean
    public SdArpManager sdArpManager(PunchingManager punchingManager) {
        return new SdArpManager(punchingManager);
    }

    @Bean
    public TunEngine tunEngine(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               Transporter transporter,
                               PunchingManager punchingManager,
                               SdArpManager sdArpManager) {
        return new TunEngine(properties, sdWanNode, transporter, punchingManager, sdArpManager);
    }
}

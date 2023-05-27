package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.MemberNodeManager;
import io.jasercloud.sdwan.support.SDWanNode;
import io.jasercloud.sdwan.support.SDWanNodeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties) {
        return new SDWanNode(properties);
    }

    @Bean
    public MemberNodeManager nodeManager() {
        return new MemberNodeManager();
    }
}

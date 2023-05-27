package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.SDWanController;
import io.jasercloud.sdwan.support.SDWanControllerProperties;
import io.jasercloud.sdwan.support.SDWanNodeManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDWanController sdWanController(SDWanControllerProperties properties) {
        return new SDWanController(properties);
    }

    @Bean
    public SDWanNodeManager nodeManager() {
        return new SDWanNodeManager();
    }
}

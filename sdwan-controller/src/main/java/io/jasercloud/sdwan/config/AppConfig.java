package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.SDWanController;
import io.jasercloud.sdwan.support.SDWanControllerProperties;
import io.jasercloud.sdwan.support.SDWanControllerProcessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDWanControllerProcessHandler processHandler(SDWanControllerProperties properties) {
        return new SDWanControllerProcessHandler(properties);
    }

    @Bean
    public SDWanController sdWanController(SDWanControllerProperties properties,
                                           SDWanControllerProcessHandler processHandler) {
        SDWanController sdWanController = new SDWanController(properties, processHandler);
        return sdWanController;
    }
}

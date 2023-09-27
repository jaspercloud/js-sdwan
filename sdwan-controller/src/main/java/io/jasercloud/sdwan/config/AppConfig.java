package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.SDWanController;
import io.jasercloud.sdwan.support.SDWanControllerProperties;
import io.jasercloud.sdwan.support.SDWanProcessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDWanProcessHandler sdWanProcessHandler(SDWanControllerProperties properties) {
        SDWanProcessHandler processHandler = new SDWanProcessHandler(properties);
        return processHandler;
    }

    @Bean
    public SDWanController sdWanController(SDWanControllerProperties properties,
                                           SDWanProcessHandler processHandler) {
        SDWanController sdWanController = new SDWanController(properties, processHandler);
        return sdWanController;
    }
}

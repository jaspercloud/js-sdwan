package io.jaspercloud.sdwan.config;

import io.jaspercloud.sdwan.app.SDWanControllerService;
import io.jaspercloud.sdwan.support.NodeManager;
import io.jaspercloud.sdwan.support.SDWanController;
import io.jaspercloud.sdwan.support.SDWanControllerProcessHandler;
import io.jaspercloud.sdwan.support.SDWanControllerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanControllerProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public NodeManager nodeManager() {
        return new NodeManager();
    }

    @Bean
    public SDWanControllerProcessHandler processHandler(SDWanControllerService controllerService) {
        return new SDWanControllerProcessHandler(controllerService);
    }

    @Bean
    public SDWanController sdWanController(SDWanControllerProperties properties,
                                           SDWanControllerProcessHandler processHandler) {
        SDWanController sdWanController = new SDWanController(properties, processHandler);
        return sdWanController;
    }
}

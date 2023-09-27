package io.jasercloud.sdwan.config;

import io.jasercloud.sdwan.support.SDWanNode;
import io.jasercloud.sdwan.support.SDWanNodeProperties;
import io.jasercloud.sdwan.support.SDwanNodeProcessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public SDwanNodeProcessHandler processHandler(SDWanNodeProperties properties) {
        SDwanNodeProcessHandler processHandler = new SDwanNodeProcessHandler(properties);
        return processHandler;
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties,
                               SDwanNodeProcessHandler processHandler) {
        return new SDWanNode(properties, processHandler);
    }
}

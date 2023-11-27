package io.jaspercloud.sdwan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("sdwan.controller")
public class SDWanControllerProperties {

    private Integer port;
    private String cidr;
    private Integer sdArpTTL;
    private String dbPath;
    private Long timeout;
}

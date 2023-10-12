package io.jaspercloud.sdwan.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties("sdwan.controller")
public class SDWanControllerProperties {

    private Integer port;
    private String cidr;
    private Integer sdArpTTL;
    private String dbPath;
}

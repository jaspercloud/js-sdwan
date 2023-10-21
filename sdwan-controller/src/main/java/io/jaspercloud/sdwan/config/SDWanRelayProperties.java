package io.jaspercloud.sdwan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("sdwan.relay")
public class SDWanRelayProperties {

    private Integer port;
    private Long timeout;
}

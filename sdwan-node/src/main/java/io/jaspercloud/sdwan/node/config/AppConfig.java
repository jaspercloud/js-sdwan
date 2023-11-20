package io.jaspercloud.sdwan.node.config;

import io.jaspercloud.sdwan.node.support.MappingManager;
import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.TunEngine;
import io.jaspercloud.sdwan.node.support.route.LinuxRouteManager;
import io.jaspercloud.sdwan.node.support.route.OsxRouteManager;
import io.jaspercloud.sdwan.node.support.route.RouteManager;
import io.jaspercloud.sdwan.node.support.route.WindowsRouteManager;
import io.jaspercloud.sdwan.node.support.tunnel.P2pManager;
import io.jaspercloud.sdwan.node.support.tunnel.RelayManager;
import io.jaspercloud.sdwan.node.support.tunnel.TunnelManager;
import io.jaspercloud.sdwan.stun.StunClient;
import io.netty.util.internal.PlatformDependent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public StunClient stunClient() throws Exception {
        return new StunClient();
    }

    @Bean
    public SDWanNode sdWanNode(SDWanNodeProperties properties) {
        return new SDWanNode(properties);
    }

    @Bean
    public MappingManager mappingManager(SDWanNodeProperties properties,
                                         StunClient stunClient) {
        return new MappingManager(properties, stunClient);
    }

    @Bean
    public RelayManager relayManager(SDWanNodeProperties properties,
                                     SDWanNode sdWanNode,
                                     StunClient stunClient) {
        return new RelayManager(properties, sdWanNode, stunClient);
    }

    @Bean
    public TunnelManager tunnelManager(SDWanNode sdWanNode,
                                       MappingManager mappingManager,
                                       P2pManager p2pManager,
                                       RelayManager relayManager) {
        return new TunnelManager(sdWanNode, mappingManager, p2pManager, relayManager);
    }

    @Bean
    public RouteManager routeManager(SDWanNode sdWanNode,
                                     TunnelManager tunnelManager) {
        RouteManager routeManager;
        if (PlatformDependent.isOsx()) {
            routeManager = new OsxRouteManager(sdWanNode, tunnelManager);
        } else if (PlatformDependent.isWindows()) {
            routeManager = new WindowsRouteManager(sdWanNode, tunnelManager);
        } else {
            routeManager = new LinuxRouteManager(sdWanNode, tunnelManager);
        }
        return routeManager;
    }

    @Bean
    public P2pManager p2pManager(SDWanNodeProperties properties,
                                 SDWanNode sdWanNode,
                                 StunClient stunClient) {
        return new P2pManager(properties, sdWanNode, stunClient);
    }

    @Bean
    public TunEngine tunEngine(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               MappingManager mappingManager,
                               RouteManager routeManager,
                               RelayManager relayManager) {
        return new TunEngine(properties, sdWanNode, mappingManager, routeManager, relayManager);
    }
}

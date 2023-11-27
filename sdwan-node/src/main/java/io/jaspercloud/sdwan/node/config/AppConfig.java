package io.jaspercloud.sdwan.node.config;

import io.jaspercloud.sdwan.node.support.TunEngine;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.support.detection.*;
import io.jaspercloud.sdwan.node.support.node.MappingManager;
import io.jaspercloud.sdwan.node.support.node.RelayClient;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.node.support.route.LinuxRouteManager;
import io.jaspercloud.sdwan.node.support.route.OsxRouteManager;
import io.jaspercloud.sdwan.node.support.route.RouteManager;
import io.jaspercloud.sdwan.node.support.route.WindowsRouteManager;
import io.jaspercloud.sdwan.node.support.tunnel.P2pManager;
import io.jaspercloud.sdwan.stun.StunClient;
import io.netty.util.internal.PlatformDependent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@EnableConfigurationProperties(SDWanNodeProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public StunClient stunClient(SDWanNodeProperties properties) throws Exception {
        return new StunClient(0, properties.getStun().getCallTimeout());
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
    public RelayClient relayClient(SDWanNodeProperties properties,
                                   SDWanNode sdWanNode,
                                   StunClient stunClient) {
        return new RelayClient(properties, sdWanNode, stunClient);
    }

    @Bean
    public ConnectionManager connectionManager(SDWanNodeProperties properties,
                                               SDWanNode sdWanNode,
                                               StunClient stunClient,
                                               RelayClient relayClient,
                                               MappingManager mappingManager,
                                               P2pManager p2pManager) {
        return new ConnectionManager(properties, sdWanNode, stunClient, relayClient, mappingManager, p2pManager);
    }

    @Bean
    public RouteManager routeManager(SDWanNode sdWanNode) {
        RouteManager routeManager;
        if (PlatformDependent.isOsx()) {
            routeManager = new OsxRouteManager(sdWanNode);
        } else if (PlatformDependent.isWindows()) {
            routeManager = new WindowsRouteManager(sdWanNode);
        } else {
            routeManager = new LinuxRouteManager(sdWanNode);
        }
        return routeManager;
    }

    @Bean
    public HostP2pDetection hostP2pDetection(SDWanNodeProperties properties,
                                             StunClient stunClient) {
        return new HostP2pDetection(properties, stunClient);
    }

    @Bean
    public SrflxP2pDetection srflxP2pDetection(SDWanNodeProperties properties,
                                               StunClient stunClient) {
        return new SrflxP2pDetection(properties, stunClient);
    }

    @Bean
    public PrflxP2pDetection prflxP2pDetection(SDWanNodeProperties properties,
                                               StunClient stunClient) {
        return new PrflxP2pDetection(properties, stunClient);
    }

    @Bean
    public RelayP2pDetection relayP2pDetection(RelayClient relayClient) {
        return new RelayP2pDetection(relayClient);
    }

    @Bean
    public P2pManager p2pManager(SDWanNodeProperties properties,
                                 SDWanNode sdWanNode,
                                 StunClient stunClient,
                                 RelayClient relayClient,
                                 ObjectProvider<List<P2pDetection>> provider) {
        List<P2pDetection> detectionList = provider.getIfAvailable();
        P2pManager p2pManager = new P2pManager(properties, sdWanNode, stunClient, relayClient);
        detectionList.forEach(item -> {
            p2pManager.addP2pDetection(item);
        });
        return p2pManager;
    }

    @Bean
    public TunEngine tunEngine(SDWanNodeProperties properties,
                               SDWanNode sdWanNode,
                               StunClient stunClient,
                               RelayClient relayClient,
                               MappingManager mappingManager,
                               RouteManager routeManager,
                               ConnectionManager connectionManager) {
        return new TunEngine(properties, sdWanNode, stunClient, relayClient, mappingManager, routeManager, connectionManager);
    }
}

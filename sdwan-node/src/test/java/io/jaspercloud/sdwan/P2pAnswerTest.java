package io.jaspercloud.sdwan;


import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.connection.ConnectionDataHandler;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.node.support.connection.PeerConnection;
import io.jaspercloud.sdwan.node.support.detection.HostP2pDetection;
import io.jaspercloud.sdwan.node.support.detection.PrflxP2pDetection;
import io.jaspercloud.sdwan.node.support.detection.RelayP2pDetection;
import io.jaspercloud.sdwan.node.support.detection.SrflxP2pDetection;
import io.jaspercloud.sdwan.node.support.node.MappingManager;
import io.jaspercloud.sdwan.node.support.node.RelayClient;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.node.support.tunnel.P2pManager;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.jaspercloud.sdwan.stun.StunClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class P2pAnswerTest {

    public static void main(String[] args) throws Exception {
        SDWanNodeProperties properties = new SDWanNodeProperties();
        properties.setController(new SDWanNodeProperties.Controller());
        properties.setStun(new SDWanNodeProperties.Stun());
        properties.setRelay(new SDWanNodeProperties.Relay());
        properties.getController().setAddress("127.0.0.1:51002");
        properties.getStun().setAddress("stun.miwifi.com:3478");
        properties.getRelay().setAddress("127.0.0.1:51003");
        //sdWanNode
        SDWanNode sdWanNode = new SDWanNode(properties);
        sdWanNode.afterPropertiesSet();
        InetSocketAddress sdWanNodeLocalAddress = (InetSocketAddress) sdWanNode.getChannel().localAddress();
        //stunClient
        StunClient stunClient = new StunClient(3000);
        stunClient.afterPropertiesSet();
        InetSocketAddress stunClientLocalAddress = (InetSocketAddress) stunClient.getChannel().localAddress();
        //relayClient
        RelayClient relayClient = new RelayClient(properties, sdWanNode, stunClient);
        relayClient.afterPropertiesSet();
        //mappingManager
        MappingManager mappingManager = new MappingManager(properties, stunClient);
        mappingManager.afterPropertiesSet();
        MappingAddress mappingAddress = mappingManager.getMappingAddress();
        //p2pManager
        P2pManager p2pManager = new P2pManager(properties, sdWanNode, stunClient, relayClient);
        p2pManager.addP2pDetection(new HostP2pDetection(properties, stunClient));
        p2pManager.addP2pDetection(new SrflxP2pDetection(properties, stunClient));
        p2pManager.addP2pDetection(new PrflxP2pDetection(properties, stunClient));
        p2pManager.addP2pDetection(new RelayP2pDetection(relayClient));
        p2pManager.afterPropertiesSet();
        ConnectionManager connectionManager = new ConnectionManager(properties, sdWanNode, stunClient, relayClient, mappingManager, p2pManager);
        connectionManager.addConnectionDataHandler(new ConnectionDataHandler() {
            @Override
            public void onData(PeerConnection connection, SDWanProtos.RoutePacket packet) {
                System.out.println();
            }
        });
        connectionManager.afterPropertiesSet();
        //address
        String host = UriComponentsBuilder.newInstance()
                .scheme("host")
                .host(sdWanNodeLocalAddress.getHostString())
                .port(stunClientLocalAddress.getPort())
                .build().toString();
        String srflx = UriComponentsBuilder.newInstance()
                .scheme("srflx")
                .host(mappingAddress.getMappingAddress().getHostString())
                .port(mappingAddress.getMappingAddress().getPort())
                .queryParam("mappingType", mappingAddress.getMappingType().name())
                .build().toString();
        String relay = UriComponentsBuilder.newInstance()
                .scheme("relay")
                .host(properties.getRelay().getAddress().getHostString())
                .port(properties.getRelay().getAddress().getPort())
                .queryParam("token", relayClient.getRelayToken())
                .build().toString();
        SDWanProtos.RegResp regResp = sdWanNode.regist("fa:50:03:01:f8:02", Arrays.asList(host, srflx, relay));
        String vip = regResp.getVip();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

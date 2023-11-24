package io.jaspercloud.sdwan;


import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.support.node.MappingManager;
import io.jaspercloud.sdwan.node.support.node.RelayClient;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.detection.HostP2pDetection;
import io.jaspercloud.sdwan.node.support.detection.PrflxP2pDetection;
import io.jaspercloud.sdwan.node.support.detection.RelayP2pDetection;
import io.jaspercloud.sdwan.node.support.detection.SrflxP2pDetection;
import io.jaspercloud.sdwan.node.support.tunnel.P2pManager;
import io.jaspercloud.sdwan.node.support.connection.PeerConnection;
import io.jaspercloud.sdwan.node.support.connection.ConnectionManager;
import io.jaspercloud.sdwan.stun.MappingAddress;
import io.jaspercloud.sdwan.stun.StunClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class P2pOfferTest {

    public static void main(String[] args) throws Exception {
        SDWanNodeProperties properties = new SDWanNodeProperties();
        properties.setControllerServer("127.0.0.1:51002");
        properties.setStunServer("stun.miwifi.com:3478");
        properties.setRelayServer("127.0.0.1:51003");
        //sdWanNode
        SDWanNode sdWanNode = new SDWanNode(properties);
        sdWanNode.afterPropertiesSet();
        InetSocketAddress sdWanNodeLocalAddress = (InetSocketAddress) sdWanNode.getChannel().localAddress();
        //stunClient
        StunClient stunClient = new StunClient();
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
        p2pManager.addP2pDetection(new HostP2pDetection(stunClient));
        p2pManager.addP2pDetection(new SrflxP2pDetection(stunClient));
        p2pManager.addP2pDetection(new PrflxP2pDetection(stunClient));
        p2pManager.addP2pDetection(new RelayP2pDetection(relayClient));
        p2pManager.afterPropertiesSet();
        ConnectionManager connectionManager = new ConnectionManager(properties, sdWanNode, stunClient, relayClient, mappingManager, p2pManager);
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
                .host(properties.getRelayServer().getHostString())
                .port(properties.getRelayServer().getPort())
                .queryParam("token", relayClient.getRelayToken())
                .build().toString();
        SDWanProtos.RegResp regResp = sdWanNode.regist("fa:50:03:01:f8:01", Arrays.asList(host, srflx, relay));
        String vip = regResp.getVip();
        SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.newBuilder()
                .setSrcIP("1")
                .setDstIP("2")
                .setPayload(ByteString.EMPTY)
                .build();
        SDWanProtos.RoutePacket routePacket = SDWanProtos.RoutePacket.newBuilder()
                .setSrcVIP(vip)
                .setDstVIP("10.1.13.254")
                .setPayload(ipPacket)
                .build();
        PeerConnection peerConnection = connectionManager.getConnection(routePacket.getSrcVIP(), routePacket.getDstVIP()).get();
        peerConnection.send(routePacket);
        System.out.println("send");
        System.out.println();
    }
}

package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.node.SDWanNode;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

public class SDWanNodeTest {

    public static void main(String[] args) throws Exception {
        SDWanNodeProperties properties = new SDWanNodeProperties();
        properties.setController(new SDWanNodeProperties.Controller());
        properties.setStun(new SDWanNodeProperties.Stun());
        properties.setRelay(new SDWanNodeProperties.Relay());
        properties.getController().setAddress("127.0.0.1:51002");
        properties.getController().setConnectTimeout(3000);
        properties.getController().setCallTimeout(3000L);
        properties.getStun().setAddress("stun.miwifi.com:3478");
        properties.getRelay().setAddress("127.0.0.1:51003");
        //sdWanNode
        SDWanNode sdWanNode = new SDWanNode(properties);
        sdWanNode.afterPropertiesSet();
        //address
        String host = UriComponentsBuilder.newInstance()
                .scheme("host")
                .host("127.0.0.1")
                .port(80)
                .build().toString();
        SDWanProtos.RegResp regResp = sdWanNode.regist("fa:50:03:01:14:01", Arrays.asList(host));
        System.out.println();
    }
}

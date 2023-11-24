package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.node.config.SDWanNodeProperties;
import io.jaspercloud.sdwan.node.support.node.MappingManager;
import io.jaspercloud.sdwan.stun.StunClient;

import java.util.concurrent.CountDownLatch;

public class MappingManagerTest {

    public static void main(String[] args) throws Exception {
        SDWanNodeProperties properties = new SDWanNodeProperties();
        properties.setStun(new SDWanNodeProperties.Stun());
        properties.getStun().setAddress("stun.miwifi.com:3478");
        StunClient stunClient = new StunClient(3000);
        stunClient.afterPropertiesSet();
        MappingManager mappingManager = new MappingManager(properties, stunClient);
        mappingManager.afterPropertiesSet();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

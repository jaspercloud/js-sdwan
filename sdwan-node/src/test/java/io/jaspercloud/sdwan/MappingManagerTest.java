package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.node.support.MappingManager;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.StunClient;

import java.util.concurrent.CountDownLatch;

public class MappingManagerTest {

    public static void main(String[] args) throws Exception {
        SDWanNodeProperties properties = new SDWanNodeProperties();
        properties.setStunServer("stun.miwifi.com:3478");
        StunClient stunClient = new StunClient();
        stunClient.afterPropertiesSet();
        MappingManager mappingManager = new MappingManager(properties, stunClient);
        mappingManager.afterPropertiesSet();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

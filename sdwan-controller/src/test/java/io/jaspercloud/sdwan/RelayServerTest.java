package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.support.RelayServer;

import java.util.concurrent.CountDownLatch;

public class RelayServerTest {

    public static void main(String[] args) throws Exception {
        SDWanRelayProperties properties = new SDWanRelayProperties();
        properties.setPort(888);
        properties.setTimeout(5000L);
        RelayServer relayServer = new RelayServer(properties);
        relayServer.afterPropertiesSet();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

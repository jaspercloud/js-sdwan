package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.adapter.server.RelayHandler;
import io.jaspercloud.sdwan.adapter.server.RelayServer;
import io.jaspercloud.sdwan.config.SDWanRelayProperties;

import java.util.concurrent.CountDownLatch;

public class RelayServerTest {

    public static void main(String[] args) throws Exception {
        SDWanRelayProperties properties = new SDWanRelayProperties();
        properties.setPort(888);
        properties.setTimeout(5000L);
        RelayServer relayServer = new RelayServer(properties, new RelayHandler());
        relayServer.afterPropertiesSet();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

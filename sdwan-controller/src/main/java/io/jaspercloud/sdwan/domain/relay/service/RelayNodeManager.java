package io.jaspercloud.sdwan.domain.relay.service;

import io.jaspercloud.sdwan.config.SDWanRelayProperties;
import io.jaspercloud.sdwan.domain.relay.vo.RelayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RelayNodeManager implements InitializingBean {

    private SDWanRelayProperties properties;
    private Map<String, RelayNode> channelMap = new ConcurrentHashMap<>();

    public RelayNodeManager(SDWanRelayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            while (true) {
                Iterator<Map.Entry<String, RelayNode>> iterator = channelMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    try {
                        Map.Entry<String, RelayNode> next = iterator.next();
                        RelayNode node = next.getValue();
                        long diffTime = System.currentTimeMillis() - node.getLastTime();
                        if (diffTime > properties.getTimeout()) {
                            iterator.remove();
                        }
                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "relay-heart-check").start();
    }

    public void addNode(String relayToken, InetSocketAddress address) {
        channelMap.put(relayToken, new RelayNode(address));
    }

    public RelayNode getNode(String relayToken) {
        RelayNode node = channelMap.get(relayToken);
        return node;
    }
}

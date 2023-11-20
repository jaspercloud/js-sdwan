package io.jaspercloud.sdwan.node.support.tunnel;

import io.jaspercloud.sdwan.node.support.SDWanNode;
import io.jaspercloud.sdwan.node.support.SDWanNodeProperties;
import io.jaspercloud.sdwan.stun.StunClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class P2pManager implements InitializingBean {

    private SDWanNodeProperties properties;
    private SDWanNode sdWanNode;
    private StunClient stunClient;
    private Map<InetSocketAddress, DataTunnel> tunnelMap = new ConcurrentHashMap<>();

    public P2pManager(SDWanNodeProperties properties,
                      SDWanNode sdWanNode,
                      StunClient stunClient) {
        this.properties = properties;
        this.sdWanNode = sdWanNode;
        this.stunClient = stunClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread tunnelHeartThread = new Thread(() -> {
            while (true) {
                for (Map.Entry<InetSocketAddress, DataTunnel> entry : tunnelMap.entrySet()) {
                    InetSocketAddress addr = entry.getKey();
                    DataTunnel dataTunnel = entry.getValue();
                    dataTunnel.check()
                            .whenComplete((result, throwable) -> {
                                if (null == throwable) {
                                    return;
                                }
                                tunnelMap.remove(addr);
                                log.error("punchingHeartTimout: {}", addr);
                            });
                }
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "p2p-tunnel-heart");
        tunnelHeartThread.setDaemon(true);
        tunnelHeartThread.start();
    }

    public CompletableFuture<InetSocketAddress> punch(InetSocketAddress srcAddr, InetSocketAddress dstAddr) {
        return null;
    }

    public void addTunnel(InetSocketAddress address, DataTunnel dataTunnel) {
        tunnelMap.put(address, dataTunnel);
    }
}

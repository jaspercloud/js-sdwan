package io.jaspercloud.sdwan.domain.relay.vo;

import java.net.InetSocketAddress;

public class RelayNode {

    private InetSocketAddress targetAddress;
    private long lastTime = System.currentTimeMillis();

    public InetSocketAddress getTargetAddress() {
        return targetAddress;
    }

    public long getLastTime() {
        return lastTime;
    }

    public RelayNode(InetSocketAddress targetAddress) {
        this.targetAddress = targetAddress;
    }
}

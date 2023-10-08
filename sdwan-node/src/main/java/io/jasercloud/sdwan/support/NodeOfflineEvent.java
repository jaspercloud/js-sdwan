package io.jasercloud.sdwan.support;

import org.springframework.context.ApplicationEvent;

public class NodeOfflineEvent extends ApplicationEvent {

    private String vip;

    public String getVip() {
        return vip;
    }

    public NodeOfflineEvent(Object source, String vip) {
        super(source);
        this.vip = vip;
    }

}

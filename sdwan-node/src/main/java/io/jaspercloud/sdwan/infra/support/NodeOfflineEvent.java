package io.jaspercloud.sdwan.infra.support;

import org.springframework.context.ApplicationEvent;

public class NodeOfflineEvent extends ApplicationEvent {

    private String ip;

    public String getIp() {
        return ip;
    }

    public NodeOfflineEvent(Object source, String ip) {
        super(source);
        this.ip = ip;
    }

}

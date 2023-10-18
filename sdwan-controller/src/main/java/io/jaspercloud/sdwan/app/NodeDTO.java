package io.jaspercloud.sdwan.app;

import io.jaspercloud.sdwan.infra.support.NodeType;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class NodeDTO {

    private Long id;
    private NodeType nodeType;
    private String vip;
    private String macAddress;
    private String remark;
    private String mapping;
    private String filtering;
    private String relayId;
    private InetSocketAddress mappingAddress;
    private Boolean online;
}

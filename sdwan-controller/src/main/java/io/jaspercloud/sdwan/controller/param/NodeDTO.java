package io.jaspercloud.sdwan.controller.param;

import io.jaspercloud.sdwan.support.NodeType;
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
    private InetSocketAddress mappingAddress;
    private InetSocketAddress relayAddress;
    private Boolean online;
}

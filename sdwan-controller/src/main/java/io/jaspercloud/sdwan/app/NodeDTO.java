package io.jaspercloud.sdwan.app;

import io.jaspercloud.sdwan.infra.support.NodeType;
import lombok.Data;

@Data
public class NodeDTO {

    private Long id;
    private NodeType nodeType;
    private String vip;
    private String macAddress;
    private String remark;
    private Boolean online;
}

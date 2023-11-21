package io.jaspercloud.sdwan.controller.param;

import io.jaspercloud.sdwan.support.NodeType;
import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {

    private Long id;
    private NodeType nodeType;
    private String vip;
    private String macAddress;
    private String remark;
    private List<String> addressList;
    private Boolean online;
}

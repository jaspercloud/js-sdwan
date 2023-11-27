package io.jaspercloud.sdwan.adapter.controller.param;

import io.jaspercloud.sdwan.domain.control.vo.NodeType;
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

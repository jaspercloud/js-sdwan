package io.jaspercloud.sdwan.app;

import lombok.Data;

@Data
public class NodeDTO {

    private Long id;
    private String vip;
    private String macAddress;
    private String remark;
    private Boolean online;
}

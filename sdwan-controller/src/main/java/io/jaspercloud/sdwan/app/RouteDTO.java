package io.jaspercloud.sdwan.app;

import lombok.Data;

@Data
public class RouteDTO {

    private Long id;
    private Long meshId;
    private String destination;
    private String nexthop;
    private String remark;
}

package io.jaspercloud.sdwan.controller.param;

import lombok.Data;

@Data
public class RouteDTO {

    private Long id;
    private Long meshId;
    private String destination;
    private String nexthop;
    private String remark;
}

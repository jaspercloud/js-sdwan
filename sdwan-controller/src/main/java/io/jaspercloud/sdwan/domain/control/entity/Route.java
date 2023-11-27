package io.jaspercloud.sdwan.domain.control.entity;

import io.jaspercloud.sdwan.Cidr;
import lombok.Data;

@Data
public class Route {

    private Long id;
    private String destination;
    private Long meshId;
    private String remark;

    public void setDestination(String destination) {
        Cidr.check(destination);
        this.destination = destination;
    }
}

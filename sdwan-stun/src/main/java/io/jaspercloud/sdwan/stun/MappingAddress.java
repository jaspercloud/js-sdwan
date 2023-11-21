package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class MappingAddress {

    private SDWanProtos.MappingTypeCode mappingType;
    private InetSocketAddress mappingAddress;

    public MappingAddress(SDWanProtos.MappingTypeCode mappingType, InetSocketAddress mappingAddress) {
        this.mappingType = mappingType;
        this.mappingAddress = mappingAddress;
    }
}
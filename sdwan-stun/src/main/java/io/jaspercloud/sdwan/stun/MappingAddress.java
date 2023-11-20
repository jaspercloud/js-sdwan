package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class MappingAddress {

    private int localPort;
    private SDWanProtos.MappingTypeCode mappingType;
    private InetSocketAddress mappingAddress;

    public MappingAddress(int localPort, SDWanProtos.MappingTypeCode mappingType, InetSocketAddress mappingAddress) {
        this.localPort = localPort;
        this.mappingType = mappingType;
        this.mappingAddress = mappingAddress;
    }
}
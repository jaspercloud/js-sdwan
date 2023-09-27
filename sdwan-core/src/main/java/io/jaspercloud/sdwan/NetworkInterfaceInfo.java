package io.jaspercloud.sdwan;

import lombok.Data;

import java.net.InterfaceAddress;

@Data
public class NetworkInterfaceInfo {

    private InterfaceAddress interfaceAddress;
    private String hardwareAddress;
}

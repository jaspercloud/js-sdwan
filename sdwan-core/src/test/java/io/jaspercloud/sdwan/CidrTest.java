package io.jaspercloud.sdwan;

import java.util.List;

public class CidrTest {

    public static void main(String[] args) {
        Cidr cidr = Cidr.parseCidr("192.168.1.0/24");
        List<String> ipList = cidr.getIpList();
        List<String> availableIpList = cidr.getAvailableIpList();
        System.out.println();
    }
}

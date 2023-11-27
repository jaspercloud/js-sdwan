package io.jaspercloud.sdwan;

import org.junit.jupiter.api.Test;

import java.util.List;

public class CidrTest {

    @Test
    public void test1() {
        Cidr cidr = Cidr.parseCidr("192.168.1.0/24");
        List<String> ipList = cidr.getIpList();
        List<String> availableIpList = cidr.getAvailableIpList();
        System.out.println();
    }

    @Test
    public void test2() {
        String cidr = Cidr.parseCidr("192.168.1.254", 24);
        System.out.println();
    }
}

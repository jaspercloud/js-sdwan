package io.jaspercloud.sdwan;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cidr {

    private List<String> ipList;
    private String maskAddress;

    private Cidr() {
    }

    public static Cidr parseCidr(String text) {
        String[] split = text.split("/");
        String address = split[0];
        int maskBits = Integer.parseInt(split[1]);
        List<String> ipList = getIpList(IPUtil.ip2int(address), maskBits);
        String maskAddress = getMaskAddress(maskBits);
        Cidr cidr = new Cidr();
        cidr.setIpList(ipList);
        cidr.setMaskAddress(maskAddress);
        return cidr;
    }

    private static List<String> getIpList(int address, int maskBits) {
        address = (address >> (32 - maskBits)) << (32 - maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        List<String> list = new ArrayList<>();
        int s = address;
        for (int n = 0; n <= count; n++) {
            String ip = IPUtil.int2ip(s);
            list.add(ip);
            s += 1;
        }
        return list;
    }

    public static String getMaskAddress(int maskBits) {
        int mask = Integer.MAX_VALUE << (32 - maskBits);
        String maskAddr = IPUtil.int2ip(mask);
        return maskAddr;
    }
}

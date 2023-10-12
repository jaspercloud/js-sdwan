package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.exception.CidrParseException;
import lombok.Data;
import sun.net.util.IPAddressUtil;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cidr {

    private List<String> ipList;
    private String address;
    private String maskAddress;
    private int maskBits;

    private Cidr() {
    }

    public static void check(String text) {
        String[] split = text.split("/");
        String address = split[0];
        if (!IPAddressUtil.isIPv4LiteralAddress(address)) {
            throw new CidrParseException("address error: " + address);
        }
        int maskBits = Integer.parseInt(split[1]);
        if (maskBits < 1 || maskBits > 32) {
            throw new CidrParseException("mask error: " + maskBits);
        }
    }

    public static Cidr parseCidr(String text) {
        String[] split = text.split("/");
        String address = split[0];
        int maskBits = Integer.parseInt(split[1]);
        List<String> ipList = getIpList(IPUtil.ip2int(address), maskBits);
        String maskAddress = getMaskAddress(maskBits);
        Cidr cidr = new Cidr();
        cidr.setAddress(address);
        cidr.setMaskBits(maskBits);
        cidr.setMaskAddress(maskAddress);
        cidr.setIpList(ipList);
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

    private static String getMaskAddress(int maskBits) {
        int mask = Integer.MAX_VALUE << (32 - maskBits);
        String maskAddr = IPUtil.int2ip(mask);
        return maskAddr;
    }

    public static boolean contains(String cidr, String ip) {
        check(cidr);
        String[] split = cidr.split("/");
        String address = split[0];
        int maskBits = Integer.parseInt(split[1]);
        int addr = IPUtil.ip2int(address);
        int minAddr = (addr >> (32 - maskBits)) << (32 - maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        int maxAddr = minAddr + count;
        int checkAddr = IPUtil.ip2int(ip);
        boolean contains = checkAddr > minAddr && checkAddr < maxAddr;
        return contains;
    }

    @Override
    public String toString() {
        return String.format("%s/%d", address, maskBits);
    }
}

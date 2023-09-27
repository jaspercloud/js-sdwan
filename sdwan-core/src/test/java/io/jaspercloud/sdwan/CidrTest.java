package io.jaspercloud.sdwan;

import java.util.ArrayList;
import java.util.List;

public class CidrTest {

    public static void main(String[] args) {
        List<String> ipList = parseIPList("192.222.0.66/30");
        System.out.println();
    }

    public static List<String> parseIPList(String cidr) {
        String[] split = cidr.split("/");
        String address = split[0];
        int maskBits = Integer.parseInt(split[1]);
        //calc
        int i = IPUtil.ip2int(address);
        i = (i >> (32 - maskBits)) << (32 - maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        List<String> list = new ArrayList<>();
        int s = i;
        for (int n = 0; n <= count; n++) {
            String ip = IPUtil.int2ip(s);
            list.add(ip);
            s += 1;
        }
        return list;
    }
}

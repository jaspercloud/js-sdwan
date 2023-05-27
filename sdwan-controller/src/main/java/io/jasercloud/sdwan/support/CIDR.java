package io.jasercloud.sdwan.support;

/**
 * A class that enables to get an IP range from CIDR specification. It supports
 * both IPv4 and IPv6.
 */
public class CIDR {

    //test
    public static void main(String[] args) {
        CIDR cidrUtils = new CIDR("192.222.0.1/16");
        boolean in = cidrUtils.inRange("192.222.0.2");
        System.out.println(in);
    }

    private int maskBits;
    private int netMask;
    private int cidrAddress;

    private String firstAddress;
    private String lastAddress;
    private String mastAddress;
    private int count;

    public String getFirstAddress() {
        return firstAddress;
    }

    public String getLastAddress() {
        return lastAddress;
    }

    public String getMastAddress() {
        return mastAddress;
    }

    public int getCount() {
        return count;
    }

    public CIDR(String netAddress) {
        if (!checkIfValidNetworkAddress(netAddress)) {
            throw new IllegalArgumentException("netAddress not valid");
        }
        String[] parts = netAddress.split("/");
        maskBits = Integer.parseInt(parts[1]);
        netMask = 0xffffffff << (32 - maskBits);
        cidrAddress = ipToInt(parts[0]) & netMask;
        firstAddress = intToIp(cidrAddress);
        count = (int) Math.pow(2, 32 - maskBits);
        lastAddress = intToIp(cidrAddress + count - 1);
        mastAddress = intToIp(netMask);
    }

    public boolean inRange(String ipAddress) {
        int inputAddress = ipToInt(ipAddress) & netMask;
        boolean result = cidrAddress == inputAddress;
        return result;
    }

    public static String intToIp(int ipAddress) {
        int octet1 = (ipAddress >> 24) & 255;
        int octet2 = (ipAddress >> 16) & 255;
        int octet3 = (ipAddress >> 8) & 255;
        int octet4 = ipAddress & 255;
        return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
    }

    private static int ipToInt(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= Integer.parseInt(parts[i]) << (24 - (8 * i));
        }
        return result;
    }

    public static boolean checkIfValidNetworkAddress(String netAddress) {
        String[] ipPart = netAddress.split("/");
        /*iPart[0]  =  A.B.C.D
         * ipPart[1] =  mask
         */
        if (ipPart.length != 2) {
            return false;
        }
        String[] ipOctets = ipPart[0].split("\\.");
        if (ipOctets.length != 4) {
            return false;
        }

        for (String octet : ipOctets) {
            if (Integer.parseInt(octet) < 0 || Integer.parseInt(octet) > 255) {
                return false;
            }
        }
        //return true if the last test is true
        return Integer.parseInt(ipPart[1]) >= 0 && Integer.parseInt(ipPart[1]) <= 32;
    }
}
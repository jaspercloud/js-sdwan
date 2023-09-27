package io.jaspercloud.sdwan;

import org.apache.commons.lang3.StringUtils;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkInterfaceTest {

    public static void main(String[] args) throws Exception {
        NetworkInterfaceInfo networkInterfaceInfo = findNetworkInterfaceInfo("192.222.0.66");
        String hardwareAddress = networkInterfaceInfo.getHardwareAddress();
        InterfaceAddress interfaceAddress = networkInterfaceInfo.getInterfaceAddress();
        String localAddress = interfaceAddress.getAddress().getHostAddress();
        short maskBits = interfaceAddress.getNetworkPrefixLength();
        List<String> ipList = getIpList(localAddress, maskBits);
        String maskAddr = parseMaskAddr(maskBits);
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setHardwareAddress(hardwareAddress);
        nodeInfo.setLocalAddress(localAddress);
        nodeInfo.setMaskAddr(maskAddr);
        nodeInfo.setIpList(ipList);
        nodeInfo.setMaskBits(maskBits);
        System.out.println();
    }

    private static NetworkInterfaceInfo findNetworkInterfaceInfo(String ip) throws SocketException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (!networkInterface.isUp()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (StringUtils.equals(interfaceAddress.getAddress().getHostAddress(), ip)) {
                    String hardwareAddress = parseHardwareAddress(networkInterface.getHardwareAddress());
                    NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                    networkInterfaceInfo.setInterfaceAddress(interfaceAddress);
                    networkInterfaceInfo.setHardwareAddress(hardwareAddress);
                    return networkInterfaceInfo;
                }
            }
        }
        return null;
    }

    private static String parseHardwareAddress(byte[] hardwareAddress) {
        StringBuilder builder = new StringBuilder();
        for (byte b : hardwareAddress) {
            builder.append(String.format("%02x", b)).append(":");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        String address = builder.toString();
        return address;
    }

    private static List<String> getIpList(String address, short maskBits) {
        int i = ip2int(address);
        i = (i >> (32 - maskBits)) << (32 - maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        List<String> list = new ArrayList<>();
        int s = i;
        for (int n = 0; n < count; n++) {
            s += 1;
            String ip = int2ip(s);
            list.add(ip);
        }
        return list;
    }

    public static int ip2int(String ip) {
        String[] split = ip.split("\\.");
        int s = 0;
        int bit = 24;
        for (String sp : split) {
            int n = Integer.parseInt(sp) << bit;
            s |= n;
            bit -= 8;
        }
        return s;
    }

    public static String int2ip(int s) {
        int d1 = s >> 24 & 0b11111111;
        int d2 = s >> 16 & 0b11111111;
        int d3 = s >> 8 & 0b11111111;
        int d4 = s & 0b11111111;
        String ip = String.format("%s.%s.%s.%s", d1, d2, d3, d4);
        return ip;
    }

    public static String parseMaskAddr(int maskBits) {
        int mask = Integer.MAX_VALUE << (32 - maskBits);
        String maskAddr = int2ip(mask);
        return maskAddr;
    }

    public static class NodeInfo {

        private String hardwareAddress;
        private String localAddress;
        private String maskAddr;
        private List<String> ipList;
        private int maskBits;

        public String getHardwareAddress() {
            return hardwareAddress;
        }

        public void setHardwareAddress(String hardwareAddress) {
            this.hardwareAddress = hardwareAddress;
        }

        public String getLocalAddress() {
            return localAddress;
        }

        public void setLocalAddress(String localAddress) {
            this.localAddress = localAddress;
        }

        public String getMaskAddr() {
            return maskAddr;
        }

        public void setMaskAddr(String maskAddr) {
            this.maskAddr = maskAddr;
        }

        public List<String> getIpList() {
            return ipList;
        }

        public void setIpList(List<String> ipList) {
            this.ipList = ipList;
        }

        public int getMaskBits() {
            return maskBits;
        }

        public void setMaskBits(int maskBits) {
            this.maskBits = maskBits;
        }
    }

    public static class NetworkInterfaceInfo {

        private InterfaceAddress interfaceAddress;
        private String hardwareAddress;

        public InterfaceAddress getInterfaceAddress() {
            return interfaceAddress;
        }

        public void setInterfaceAddress(InterfaceAddress interfaceAddress) {
            this.interfaceAddress = interfaceAddress;
        }

        public String getHardwareAddress() {
            return hardwareAddress;
        }

        public void setHardwareAddress(String hardwareAddress) {
            this.hardwareAddress = hardwareAddress;
        }
    }
}

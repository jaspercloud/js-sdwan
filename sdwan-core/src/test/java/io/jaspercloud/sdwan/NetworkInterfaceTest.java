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
        NetworkInterfaceInfo networkInterfaceInfo = findNetworkInterfaceInfo("10.1.0.5");
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
                    NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                    networkInterfaceInfo.setInterfaceAddress(interfaceAddress);
                    if (null != networkInterface.getHardwareAddress()) {
                        String hardwareAddress = parseHardwareAddress(networkInterface.getHardwareAddress());
                        networkInterfaceInfo.setHardwareAddress(hardwareAddress);
                    }
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

    public static String parseMaskAddr(int maskBits) {
        int mask = Integer.MAX_VALUE << (32 - maskBits);
        String maskAddr = IPUtil.int2ip(mask);
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

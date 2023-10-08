package io.jaspercloud.sdwan;

import org.apache.commons.lang3.StringUtils;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkInterfaceUtil {

    public static NetworkInterfaceInfo findNetworkInterfaceInfo(String ip) throws SocketException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (!networkInterface.isUp()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (StringUtils.equals(interfaceAddress.getAddress().getHostAddress(), ip)) {
                    NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                    networkInterfaceInfo.setName(networkInterface.getName());
                    networkInterfaceInfo.setIndex(networkInterface.getIndex());
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
}

package io.jaspercloud.sdwan;

public class NetworkInterfaceTest {

    public static void main(String[] args) throws Exception {
        NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo("192.222.0.66");
        System.out.println();
    }
}

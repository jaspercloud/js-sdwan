package io.jaspercloud.sdwan;

public class IPUtil {

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

    public static String bytes2ip(byte[] bytes) {
        int d1 = bytes[0] & 0b11111111;
        int d2 = bytes[1] & 0b11111111;
        int d3 = bytes[2] & 0b11111111;
        int d4 = bytes[3] & 0b11111111;
        String ip = String.format("%s.%s.%s.%s", d1, d2, d3, d4);
        return ip;
    }

    public static byte[] ip2bytes(String ip) {
        String[] split = ip.split("\\.");
        byte[] bytes = new byte[split.length];
        for (int i = 0; i < split.length; i++) {
            bytes[i] = (byte) Integer.parseInt(split[i]);
        }
        return bytes;
    }
}

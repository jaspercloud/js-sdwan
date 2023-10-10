package io.jasercloud.sdwan.tun.linux;

import io.jasercloud.sdwan.tun.ProcessUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public final class Iptables {

    private Iptables() {

    }

    public static void addFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -A FORWARD -i %s -o %s -j ACCEPT", tunName, ethName);
        int code = ProcessUtil.exec(cmd);
    }

    public static boolean queryFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = "iptables -t filter -L FORWARD -n -v";
        List<String> list = ProcessUtil.query(cmd);
        for (String line : list) {
            if (containsKeyword(line, tunName, ethName)) {
                return true;
            }
        }
        return false;
    }

    public static void deleteFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -D FORWARD -i %s -o %s -j ACCEPT", tunName, ethName);
        int code = ProcessUtil.exec(cmd);
    }

    public static void addNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t nat -A POSTROUTING -o %s -j MASQUERADE", ethName);
        int code = ProcessUtil.exec(cmd);
    }

    public static boolean queryNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = "iptables -t nat -L POSTROUTING -n -v";
        List<String> list = ProcessUtil.query(cmd);
        for (String line : list) {
            if (containsKeyword(line, ethName)) {
                return true;
            }
        }
        return false;
    }

    public static void deleteNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t nat -D POSTROUTING -o %s -j MASQUERADE", ethName);
        int code = ProcessUtil.exec(cmd);
    }

    private static boolean containsKeyword(String line, String... keywords) {
        for (String keyword : keywords) {
            if (!StringUtils.contains(line, keyword)) {
                return false;
            }
        }
        return true;
    }
}

package io.jasercloud.sdwan.tun.linux;

import io.jasercloud.sdwan.tun.ProcessUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public final class Iptables {

    private Iptables() {

    }

    public static void addFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -A FORWARD -i %s -o %s -j ACCEPT", tunName, ethName);
        int code = ProcessUtil.exec(cmd);
    }

    public static boolean queryFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -L -n -v | grep ACCEPT | grep %s | grep %s", tunName, ethName);
        String out = ProcessUtil.query(cmd);
        return StringUtils.isNotEmpty(out);
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
        String cmd = String.format("iptables -t nat -L -n -v | grep MASQUERADE | grep %s", ethName);
        String out = ProcessUtil.query(cmd);
        return StringUtils.isNotEmpty(out);
    }

    public static void deleteNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t nat -D POSTROUTING -o %s -j MASQUERADE", ethName);
        int code = ProcessUtil.exec(cmd);
    }
}

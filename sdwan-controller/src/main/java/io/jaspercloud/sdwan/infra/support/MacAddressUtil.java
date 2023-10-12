package io.jaspercloud.sdwan.infra.support;

import java.util.regex.Pattern;

public final class MacAddressUtil {

    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    private MacAddressUtil() {

    }

    public static boolean check(String mac) {
        boolean find = MAC_PATTERN.matcher(mac).find();
        return find;
    }
}

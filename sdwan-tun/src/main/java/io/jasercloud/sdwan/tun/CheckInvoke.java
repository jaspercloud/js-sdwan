package io.jasercloud.sdwan.tun;

import io.jaspercloud.sdwan.exception.ProcessException;

public final class CheckInvoke {

    private CheckInvoke() {

    }

    public static void check(int value, int expected) {
        if (value != expected) {
            throw new ProcessException();
        }
    }
}

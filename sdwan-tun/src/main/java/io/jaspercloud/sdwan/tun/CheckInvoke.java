package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.exception.ProcessException;

public final class CheckInvoke {

    private CheckInvoke() {

    }

    public static void check(int value, int expected) {
        if (value != expected) {
            throw new ProcessException();
        }
    }

    public static void check(int value, int... expected) {
        for (int item : expected) {
            if (value == item) {
                return;
            }
        }
        throw new ProcessException();
    }
}

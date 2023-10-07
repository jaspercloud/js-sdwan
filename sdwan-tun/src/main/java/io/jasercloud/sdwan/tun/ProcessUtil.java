package io.jasercloud.sdwan.tun;

import java.io.IOException;

public class ProcessUtil {

    public static int exec(String command) throws IOException, InterruptedException {
        int code = exec(command.split(" "));
        return code;
    }

    public static int exec(String... command) throws IOException, InterruptedException {
        int code = Runtime.getRuntime().exec(command).waitFor();
        return code;
    }
}

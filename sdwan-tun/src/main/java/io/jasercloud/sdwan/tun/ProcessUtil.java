package io.jasercloud.sdwan.tun;

import io.jaspercloud.sdwan.exception.ProcessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {

    public static String query(String command) throws IOException, InterruptedException {
        Process process = exec(command.split(" "));
        int code = process.waitFor();
        if (0 != code) {
            throw new ProcessException();
        }
        StringBuilder builder = new StringBuilder();
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"))) {
            while (null != (line = reader.readLine())) {
                builder.append(line);
            }
        }
        String out = builder.toString();
        return out;
    }

    public static int exec(String command) throws IOException, InterruptedException {
        int code = exec(command.split(" ")).waitFor();
        return code;
    }

    public static Process exec(String... command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        return process;
    }
}

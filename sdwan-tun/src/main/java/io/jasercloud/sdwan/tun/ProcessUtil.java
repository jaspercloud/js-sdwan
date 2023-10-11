package io.jasercloud.sdwan.tun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtil {

    public static List<String> query(String command) throws IOException, InterruptedException {
        Process process = exec(command.split(" "));
        process.waitFor();
        List<String> lines = new ArrayList<>();
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"))) {
            while (null != (line = reader.readLine())) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static int exec(String command) throws IOException, InterruptedException {
        int code = exec(command.split(" ")).waitFor();
        return code;
    }

    public static Process exec(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        return process;
    }
}

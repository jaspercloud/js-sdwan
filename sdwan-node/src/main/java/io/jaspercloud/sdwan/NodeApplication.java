package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tun.CheckAdmin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NodeApplication {

    public static void main(String[] args) {
        CheckAdmin.check();
        System.setProperty("io.netty.leakDetection.level", "ADVANCED");
        System.setProperty("io.netty.leakDetection.samplingInterval", "1");
        SpringApplication.run(NodeApplication.class, args);
    }

}

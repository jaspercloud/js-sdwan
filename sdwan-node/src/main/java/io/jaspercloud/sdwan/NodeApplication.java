package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.node.support.TunEngine;
import io.jaspercloud.sdwan.tun.CheckAdmin;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class NodeApplication {

    public static void main(String[] args) throws Exception {
        CheckAdmin.check();
        System.setProperty("io.netty.leakDetection.level", "ADVANCED");
        System.setProperty("io.netty.leakDetection.samplingInterval", "1");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(NodeApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        TunEngine tunEngine = context.getBean(TunEngine.class);
        tunEngine.getTunChannel().closeFuture().sync();
    }

}

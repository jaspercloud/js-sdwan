package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jasercloud.sdwan.CheckResult;
import io.jasercloud.sdwan.StunClient;
import org.slf4j.impl.StaticLoggerBinder;

import java.net.InetSocketAddress;

public class StunTest {

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        root.setLevel(Level.INFO);
        System.setProperty("io.netty.noPreferDirect", "true");
        new StunTest().run();
    }

    private InetSocketAddress local = new InetSocketAddress("0.0.0.0", 0);
    private InetSocketAddress target = new InetSocketAddress("stun.miwifi.com", 3478);

    private void run() throws Exception {
        StunClient stunClient = StunClient.boot(local);
        CheckResult checkResult = stunClient.check(target, 3000);
        System.out.println("mapping:" + checkResult.getMapping());
        System.out.println("filtering:" + checkResult.getFiltering());
        System.out.println("address:" + checkResult.getMappingAddress());
        System.out.println();
    }
}
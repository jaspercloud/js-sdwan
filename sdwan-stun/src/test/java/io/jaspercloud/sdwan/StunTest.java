package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jaspercloud.sdwan.stun.AddressAttr;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.StunClient;
import io.jaspercloud.sdwan.stun.StunPacket;
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

    private void run() throws Exception {
        StunClient stunClient1 = new StunClient(1001, 3000);
        stunClient1.afterPropertiesSet();
        StunClient stunClient2 = new StunClient(1002, 3000);
        stunClient2.afterPropertiesSet();

        StunPacket stunPacket = stunClient1.sendBind(new InetSocketAddress("127.0.0.1", 1002), 300).get();
        AddressAttr addressAttr = stunPacket.content().getAttr(AttrType.MappedAddress);
        InetSocketAddress address = addressAttr.getAddress();
        System.out.println();
    }
}
package demo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.impl.StaticLoggerBinder;

import java.util.concurrent.CountDownLatch;

public class WinTunApplication {

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger logger = loggerContext.getLogger("ROOT");
        logger.setLevel(Level.INFO);
        WinTun winTun1 = new WinTun("10.0.0.1", 24);
        winTun1.start("win1");
        WinTun winTun2 = new WinTun("10.0.0.2", 24);
        winTun2.start("win2");

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

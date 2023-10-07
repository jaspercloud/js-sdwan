//package io.jaspercloud.sdwan;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import org.slf4j.impl.StaticLoggerBinder;
//
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.concurrent.CountDownLatch;
//
//public class WinTunSocketTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//        Logger punching = loggerContext.getLogger("io.jaspercloud.sdwan");
//        punching.setLevel(Level.DEBUG);
//
//        ServerSocket serverSocket = new ServerSocket();
//        serverSocket.bind(new InetSocketAddress("10.2.0.5", 8888));
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Socket accept = serverSocket.accept();
//                    System.out.println("connected");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        Socket socket = new Socket();
//        socket.bind(new InetSocketAddress("10.1.0.5", 0));
//        socket.connect(new InetSocketAddress("10.1.0.1", 8888));
////        socket.connect(new InetSocketAddress("10.1.0.5", 8888));
//        System.out.println();
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}

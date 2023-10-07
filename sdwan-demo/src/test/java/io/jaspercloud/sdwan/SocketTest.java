//package io.jaspercloud.sdwan;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import org.slf4j.impl.StaticLoggerBinder;
//
//import java.io.*;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.util.concurrent.CountDownLatch;
//
//public class SocketTest {
//
//    public static void main(String[] args) throws Exception {
//        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
//        Logger root = loggerContext.getLogger("ROOT");
//        root.setLevel(Level.INFO);
//
//        ServerSocket serverSocket = new ServerSocket(16379);
//        try {
////            Socket socket = new Socket();
////            socket.connect(new InetSocketAddress("192.222.0.66", 16379));
////            Socket accept = serverSocket.accept();
////            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(accept.getOutputStream()));
////            printWriter.println("test");
////            printWriter.flush();
////            System.out.println();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
////            Socket socket = new Socket();
////            socket.connect(new InetSocketAddress("10.1.0.2", 16379));
////            System.out.println();
//        } catch (Exception e) {
//        }
//        try {
//            Socket socket = new Socket();
//            socket.connect(new InetSocketAddress("10.1.0.5", 16379));
//            Socket accept = serverSocket.accept();
//            OutputStream serverOutputStream = accept.getOutputStream();
//            serverOutputStream.write("test\r\n".getBytes(StandardCharsets.UTF_8));
//            serverOutputStream.flush();
////            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////            String line = reader.readLine();
////            System.out.println();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}

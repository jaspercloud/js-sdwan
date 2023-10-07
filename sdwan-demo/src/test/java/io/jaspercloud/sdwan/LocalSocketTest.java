//package io.jaspercloud.sdwan;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.net.*;
//import java.nio.charset.StandardCharsets;
//import java.util.concurrent.CountDownLatch;
//
//public class LocalSocketTest {
//
//    public static void main(String[] args) throws Exception {
////        DatagramSocket datagramSocket = new DatagramSocket();
////        InetSocketAddress address = new InetSocketAddress("192.222.0.67", 16379);
////        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
////        DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address);
////        datagramSocket.send(datagramPacket);
////        System.out.println();
//
//        ServerSocket serverSocket = new ServerSocket(16379);
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Socket socket = serverSocket.accept();
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                    String host = socket.getRemoteSocketAddress().toString();
//                    String text = reader.readLine();
//                    System.out.println(String.format("host=%s, text=%s", host, text));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        Socket socket = new Socket();
////        socket.connect(new InetSocketAddress("192.222.0.66", 16379));
////        socket.connect(new InetSocketAddress("192.222.0.65", 16379));
//        socket.connect(new InetSocketAddress("127.0.0.1", 16379));
//        System.out.println("connect");
//        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
//        for (int i = 0; i < 5; i++) {
//            printWriter.println("test");
//            printWriter.flush();
//        }
//        printWriter.close();
//        socket.close();
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}

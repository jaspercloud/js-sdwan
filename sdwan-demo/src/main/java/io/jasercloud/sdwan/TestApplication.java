package io.jasercloud.sdwan;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TestApplication {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("10.0.0.1", 80));
        Socket socket = new Socket();
        socket.bind(new InetSocketAddress("10.0.0.2", 5600));
        socket.connect(new InetSocketAddress("10.0.0.1", 80));
        Socket accept = serverSocket.accept();
        new Thread(() -> {
            while (true) {
                try {
                    InputStream inputStream = accept.getInputStream();
                    byte[] bytes = new byte[3 * 5000];
                    int read = inputStream.read(bytes);
                    System.out.println("recv: " + read);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        while (true) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[5000]);
            outputStream.flush();
            outputStream.write(new byte[5000]);
            outputStream.flush();
            outputStream.write(new byte[5000]);
            outputStream.flush();
            Thread.sleep(1000L);
        }
    }
}

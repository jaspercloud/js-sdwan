package io.jasercloud.sdwan;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TestApplicationLocalMtu {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", 80));
        Socket socket = new Socket();
//        socket.bind(new InetSocketAddress("127.0.0.1", 5600));
        socket.connect(new InetSocketAddress("127.0.0.1", 80));
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(new byte[102400]);
        outputStream.flush();
        outputStream.write(new byte[102400]);
        outputStream.flush();
        outputStream.write(new byte[102400]);
        outputStream.flush();
        Socket accept = serverSocket.accept();
        InputStream inputStream = accept.getInputStream();
        byte[] bytes = new byte[3 * 102400];
        int read = inputStream.read(bytes);
        System.out.println();
    }
}

package demo;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketTest {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("10.0.0.1", 80));
        Socket socket = new Socket();
        socket.bind(new InetSocketAddress("10.0.0.2", 5600));
        socket.connect(new InetSocketAddress("10.0.0.1", 80));
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(new byte[20480]);
        outputStream.flush();
        outputStream.write(new byte[20480]);
        outputStream.flush();
        outputStream.write(new byte[20480]);
        outputStream.flush();
        Socket accept = serverSocket.accept();
        InputStream inputStream = accept.getInputStream();
        byte[] bytes = new byte[3 * 10240];
        int read = inputStream.read(bytes);
        System.out.println();
    }
}

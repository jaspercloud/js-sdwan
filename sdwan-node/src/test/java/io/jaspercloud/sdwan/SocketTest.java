package io.jaspercloud.sdwan;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketTest {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("10.1.13.69", 1080));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        writer.println("test1");
        writer.println("test2");
        writer.println("test3");
        writer.flush();
        socket.close();
    }
}

package io.jaspercloud.sdwan;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServerTest {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(1080);
        System.out.println("server: " + 1080);
        Socket socket;
        while (null != (socket = serverSocket.accept())) {
            System.out.println("accept");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while (null != (line = reader.readLine())) {
                    System.out.println(line);
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

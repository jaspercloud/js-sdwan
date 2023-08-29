package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketTest {

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        root.setLevel(Level.INFO);
        Logger punching = loggerContext.getLogger("io.jaspercloud.sdwan");
        punching.setLevel(Level.DEBUG);

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("10.1.0.20", 8080));
        while (true) {
            System.out.println("accept");
            Socket accept = serverSocket.accept();
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(accept.getOutputStream(), "utf-8"));
            printWriter.println("hello world");
            printWriter.flush();
            accept.close();
        }
    }
}

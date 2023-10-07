//package io.jaspercloud.sdwan;
//
//import com.sun.jna.*;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Arrays;
//
//public class WinLib {
//
//    public interface WS2_32 extends Library {
//
//        WS2_32 INSTANCE = Native.load("ws2_32", WS2_32.class);
//
//        int socket(int af, int type, int protocol);
//
//        int setsockopt(int socket, int level, int optname, Pointer optval, int optlen);
//
//        int ioctlsocket(int socket, long cmd, Pointer argp);
//
//        int connect(int socket, sockaddr_in address, int addressSize);
//
//        int send(int socket, byte[] buf, int len, int flags);
//
//        int recv(int socket, byte[] buf, int len, int flags);
//
//        int sendto(int socket, byte[] buf, int len, int flags, Pointer to, int toLen);
//
//        int inet_addr(String host);
//
//        short htons(int port);
//
//        int closesocket(int socket);
//
//        int WSAGetLastError();
//    }
//
//    @Structure.FieldOrder({"sin_family", "sin_port", "sin_addr", "sin_zero"})
//    public static class sockaddr_in extends Structure {
//
//        public short sin_family;
//        public short sin_port;
//        public in_addr sin_addr;
//        public byte[] sin_zero = new byte[8];
//    }
//
//    @Structure.FieldOrder({"s_addr"})
//    public static class in_addr extends Structure {
//
//        public int s_addr;
//    }
//
//    public static final int AF_UNSPEC = 0;
//    public static final int AF_INET = 2;
//    public static final int AF_INET6 = 23;
//
//    public static final int SOCK_STREAM = 1;
//    public static final int SOCK_DGRAM = 2;
//    public static final int SOCK_RAW = 3;
//
//    public static final int IPPROTO_ICMP = 1;
//    public static final int IPPROTO_TCP = 6;
//    public static final int IPPROTO_UDP = 17;
//    public static final int IPPROTO_ICMPV6 = 58;
//
//    public static final int SOL_SOCKET = 0xffff;
//
//    public static final int SO_SNDTIMEO = 0x1005;
//    public static final int SO_RCVTIMEO = 0x1006;
//
//    public static void main(String[] args) throws Exception {
//        ServerSocket serverSocket = new ServerSocket(80);
//
//        // 服务器地址和端口
//        String server_ip = "192.222.0.66";
//        int server_port = 180;
//
//        // 创建套接字
//        int sockfd = WS2_32.INSTANCE.socket(AF_INET, SOCK_STREAM, 0); // AF_INET: 2, SOCK_STREAM: 1, Protocol: 0
//
//        if (sockfd == -1) {
//            int error = WS2_32.INSTANCE.WSAGetLastError();
//            System.err.println("套接字创建失败，错误代码: " + error);
//            return;
//        }
//
//        // 设置服务器地址信息
//        sockaddr_in serverAddr = new sockaddr_in();
//        serverAddr.sin_family = AF_INET;
//        serverAddr.sin_port = WS2_32.INSTANCE.htons(server_port);
//        serverAddr.sin_addr.s_addr = WS2_32.INSTANCE.inet_addr(server_ip);
//
////        Memory pointer = new Memory(4);
////        pointer.setInt(0, 60 * 1000);
////        WS2_32.INSTANCE.setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, pointer, (int) pointer.size());
////        WS2_32.INSTANCE.setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, pointer, (int) pointer.size());
//        // 连接到服务器
//        int result = WS2_32.INSTANCE.connect(sockfd, serverAddr, serverAddr.size());
//        if (result == -1) {
//            int error = WS2_32.INSTANCE.WSAGetLastError();
//            System.err.println("连接失败，错误代码: " + error);
//            WS2_32.INSTANCE.closesocket(sockfd);
//            return;
//        }
//
//        String message = "Hello World\n";
//        byte[] messageBytes = message.getBytes();
//        int bytesSent = WS2_32.INSTANCE.send(sockfd, messageBytes, messageBytes.length, 0);
//
//        Socket accept = serverSocket.accept();
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
//        String line = reader.readLine();
//        PrintWriter writer = new PrintWriter(new OutputStreamWriter(accept.getOutputStream()));
//        writer.print(line);
//        writer.flush();
//
//        byte[] tmp = new byte[1024];
//        int recv = WS2_32.INSTANCE.recv(sockfd, tmp, tmp.length, 0);
//        byte[] bytes = Arrays.copyOf(tmp, recv);
//        String recvLine = new String(bytes);
//
//        int closesocket = WS2_32.INSTANCE.closesocket(sockfd);
//        System.out.println();
//    }
//}

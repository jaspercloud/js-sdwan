package io.jaspercloud.sdwan;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public class RawSocket {

    public static final int AF_UNSPEC = 0;
    public static final int AF_INET = 2;
    public static final int AF_INET6 = 23;

    public static final int SOCK_STREAM = 1;
    public static final int SOCK_DGRAM = 2;
    public static final int SOCK_RAW = 3;

    public static final int IPPROTO_IP = 0;
    public static final int IPPROTO_ICMP = 1;
    public static final int IPPROTO_IPV4 = 4;
    public static final int IPPROTO_TCP = 6;
    public static final int IPPROTO_UDP = 17;
    public static final int IPPROTO_ICMPV6 = 58;
    public static final int IPPROTO_RAW = 255;
    public static final int SOL_SOCKET = 0xffff;

    public static final int IP_HDRINCL = 2;
    public static final int SO_SNDTIMEO = 0x1005;
    public static final int SO_RCVTIMEO = 0x1006;

    public interface WS2_32 extends Library {

        WS2_32 INSTANCE = Native.load("ws2_32", WS2_32.class);

        int socket(int af, int type, int protocol);

        int setsockopt(int socket, int level, int optname, Pointer optval, int optlen);

        int ioctlsocket(int socket, long cmd, Pointer argp);

        int connect(int socket, sockaddr_in address, int addressSize);

        int bind(int socket, sockaddr_in address, int addressSize);

        int send(int socket, byte[] buf, int len, int flags);

        int recv(int socket, byte[] buf, int len, int flags);

        int recvfrom(int socket, byte[] buf, int len, int flags, sockaddr_in from, IntByReference fromLen);

        int sendto(int socket, byte[] buf, int len, int flags, sockaddr to, int toLen);

        int inet_addr(String host);

        short htons(int port);

        int closesocket(int socket);

        int WSAGetLastError();
    }

    @Structure.FieldOrder({"sin_family", "sin_port", "sin_addr", "sin_zero"})
    public static class sockaddr_in extends Structure {

        public short sin_family;
        public short sin_port;
        public in_addr sin_addr;
        public byte[] sin_zero = new byte[8];
    }

    @Structure.FieldOrder({"s_addr"})
    public static class in_addr extends Structure {

        public int s_addr;
    }

    @Structure.FieldOrder({"sa_family", "sa_data"})
    public static class sockaddr extends Structure {

        public short sa_family;
        public char[] sa_data;
    }

    private int fd;

    public RawSocket(int af, int type, int protocol) {
        fd = WS2_32.INSTANCE.socket(af, type, protocol);
    }

    public int setsockopt(int level, int optname, Pointer optval, int optlen) {
        int ret = WS2_32.INSTANCE.setsockopt(fd, level, optname, optval, optlen);
        return ret;
    }

    public int bind(String addr, int port) {
        sockaddr_in sockaddrIn = new sockaddr_in();
        sockaddrIn.sin_family = AF_INET;
        sockaddrIn.sin_port = WS2_32.INSTANCE.htons(port);
        sockaddrIn.sin_addr.s_addr = WS2_32.INSTANCE.inet_addr(addr);
        int ret = WS2_32.INSTANCE.bind(fd, sockaddrIn, sockaddrIn.size());
        return ret;
    }

    public int write(String addr, byte[] buf) {
        sockaddr sockaddr = new sockaddr();
        sockaddr.sa_family = AF_INET;
        sockaddr.sa_data = addr.toCharArray();
        int ret = WS2_32.INSTANCE.sendto(fd, buf, buf.length, 0, sockaddr, sockaddr.size());
        return ret;
    }

    public int read(byte[] buf) {
        int ret = WS2_32.INSTANCE.recv(fd, buf, buf.length, 0);
        return ret;
    }

    public int recvfrom(byte[] bytes) {
        sockaddr_in sockaddrIn = new sockaddr_in();
        IntByReference fromLen = new IntByReference(sockaddrIn.size());
        int ret = WS2_32.INSTANCE.recvfrom(fd, bytes, bytes.length, 0, sockaddrIn, fromLen);
        return ret;
    }

    public int close() {
        int ret = WS2_32.INSTANCE.closesocket(fd);
        return ret;
    }

}

package io.jaspercloud.sdwan.tun.linux;

import com.sun.jna.*;

public class NativeLinuxApi {

    static {
        try {
            Native.register(NativeLinuxApi.class, Platform.C_LIBRARY_NAME);
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final int O_RDWR = 2;
    public static final short IFF_TUN = 0x0001;
    public static final short IFF_NO_PI = 0x1000;

    public static final int F_GETFL = 3;
    public static final int F_SETFL = 4;
    public static final int O_NONBLOCK = 2048;

    public static final NativeLong TUNSETIFF = new NativeLong(0x400454caL);

    public static native int geteuid() throws LastErrorException;

    public static native int open(String path, int flags) throws LastErrorException;

    public static native int fcntl(int fd, int cmd, long arg) throws LastErrorException;

    public static native int close(int fd) throws LastErrorException;

    public static native int read(int fd, byte[] buf, int nbytes) throws LastErrorException;

    public static native int write(int fd, byte[] buf, int nbytes) throws LastErrorException;

    public static native int ioctl(int fd, NativeLong request, Structure argp) throws LastErrorException;

    public static native int select(int nfds, FdSet readfds, Pointer writefds, Pointer exceptfds, Timeval timeout);
}

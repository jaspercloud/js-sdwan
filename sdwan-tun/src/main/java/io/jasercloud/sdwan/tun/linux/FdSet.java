package io.jasercloud.sdwan.tun.linux;

import com.sun.jna.Structure;

@Structure.FieldOrder({"fds_bits"})
public class FdSet extends Structure {

    public static final int FD_SETSIZE = 1024;

    public int[] fds_bits = new int[(FD_SETSIZE + 31) / 32];

    public void FD_SET(int fd) {
        fds_bits[fd / 32] |= (1 << (fd % 32));
    }

    public void FD_CLR(int fd) {
        fds_bits[fd / 32] &= ~(1 << (fd % 32));
    }

    public boolean FD_ISSET(int fd) {
        return (fds_bits[fd / 32] & (1 << (fd % 32))) != 0;
    }
}
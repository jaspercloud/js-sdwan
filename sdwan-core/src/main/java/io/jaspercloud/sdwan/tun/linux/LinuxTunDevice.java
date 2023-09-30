package io.jaspercloud.sdwan.tun.linux;

import com.sun.jna.*;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class LinuxTunDevice extends TunDevice {

    public static class LinuxC {

        static {
            try {
                Native.register(LinuxC.class, Platform.C_LIBRARY_NAME);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public static final int O_RDWR = 2;
        public static final int IFNAMSIZ = 16;
        public static final short IFF_TUN = 0x0001;
        public static final short IFF_NO_PI = 0x1000;

        public static final int F_GETFL = 3;
        public static final int F_SETFL = 4;
        public static final int O_NONBLOCK = 2048;

        public static final NativeLong TUNSETIFF = new NativeLong(0x400454caL);

        public static native int open(String path, int flags) throws LastErrorException;

        public static native int fcntl(int fd, int cmd, long arg) throws LastErrorException;

        public static native int close(int fd) throws LastErrorException;

        public static native int read(int fd, byte[] buf, int nbytes) throws LastErrorException;

        public static native int write(int fd, byte[] buf, int nbytes) throws LastErrorException;

        public static native int ioctl(int fd, NativeLong request, Structure argp) throws LastErrorException;

        public static native int select(int nfds, FdSet readfds, Pointer writefds, Pointer exceptfds, Timeval timeout);

        @Structure.FieldOrder({"ifr_name", "ifr_ifru"})
        public static class Ifreq extends Structure {
            public byte[] ifr_name;
            public FfrIfru ifr_ifru;

            public Ifreq(final String ifr_name, final short flags) {
                this.ifr_name = new byte[IFNAMSIZ];
                if (ifr_name != null) {
                    final byte[] bytes = ifr_name.getBytes(US_ASCII);
                    System.arraycopy(bytes, 0, this.ifr_name, 0, bytes.length);
                }
                this.ifr_ifru.setType("ifru_flags");
                this.ifr_ifru.ifru_flags = flags;
            }

            public static class FfrIfru extends Union {
                public short ifru_flags;
            }
        }

        @Structure.FieldOrder({"fds_bits"})
        public static class FdSet extends Structure {

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

        @Structure.FieldOrder({"tv_sec", "tv_usec"})
        public static class Timeval extends Structure {

            public long tv_sec;
            public long tv_usec;
        }
    }

    private int fd;
    private int mtu = 65535;
    private LinuxC.FdSet fdSet;
    private LinuxC.Timeval timeval;
    private boolean closing = false;

    public LinuxTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        fd = LinuxC.open("/dev/net/tun", LinuxC.O_RDWR);
        int flags = LinuxC.fcntl(fd, LinuxC.F_GETFL, 0);
        int noblock = LinuxC.fcntl(fd, LinuxC.F_SETFL, flags | LinuxC.O_NONBLOCK);
        fdSet = new LinuxC.FdSet();
        fdSet.FD_SET(fd);
        timeval = new LinuxC.Timeval();
        timeval.tv_sec = 5;
        LinuxC.Ifreq ifreq = new LinuxC.Ifreq(getName(), (short) (LinuxC.IFF_TUN | LinuxC.IFF_NO_PI));
        LinuxC.ioctl(fd, LinuxC.TUNSETIFF, ifreq);
        setActive(true);
    }

    @Override
    public int getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        int addAddr = ProcessUtil.exec(String.format("/sbin/ip addr add %s/%s dev %s", addr, netmaskPrefix, getName()));
        int up = ProcessUtil.exec(String.format("/sbin/ip link set dev %s up", getName()));
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        int setMtu = ProcessUtil.exec(String.format("ifconfig %s mtu %s up", getName(), mtu));
        this.mtu = mtu;
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            int select = LinuxTunDevice.LinuxC.select(fd + 1, fdSet, null, null, timeval);
            if (-1 == select) {
                throw new ProcessException("select -1");
            }
            if (fdSet.FD_ISSET(fd)) {
                byte[] bytes = new byte[mtu];
                int read = LinuxC.read(fd, bytes, bytes.length);
                if (read <= 0) {
                    continue;
                }
                ByteBuf byteBuf = alloc.buffer(read);
                byteBuf.writeBytes(bytes, 0, read);
                return byteBuf;
            }
        }
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        if (closing) {
            throw new ProcessException("Device is closed.");
        }
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        LinuxC.write(fd, bytes, bytes.length);
    }

    @Override
    public void addRoute(String route, String ip) throws Exception {
        String cmd = String.format("ip route add %s via %s", route, ip);
        int addRoute = ProcessUtil.exec(cmd);
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        int close = LinuxC.close(fd);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

    public static void main(String[] args) throws Exception {
        LinuxTunDevice tunDevice = new LinuxTunDevice("tun", "sdwan", UUID.randomUUID().toString());
        tunDevice.open();
        tunDevice.setIP("192.168.1.1", 24);
        tunDevice.setMTU(1500);
        UnpooledByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        while (true) {
            ByteBuf byteBuf = tunDevice.readPacket(allocator);
            byteBuf.markReaderIndex();
            byte version = (byte) (byteBuf.readUnsignedByte() >> 4);
            byteBuf.resetReaderIndex();
            if (4 == version) {
                Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
                System.out.println(String.format("%s -> %s", ipv4Packet.getSrcIP(), ipv4Packet.getDstIP()));
                byteBuf.resetReaderIndex();
                tunDevice.writePacket(allocator, byteBuf);
            }
        }
    }
}

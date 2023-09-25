package io.jaspercloud.sdwan.tun.linux;

import com.sun.jna.*;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.drasyl.channel.tun.jna.shared.If;
import org.drasyl.channel.tun.jna.shared.LibC;

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

        public static final NativeLong TUNSETIFF = new NativeLong(0x400454caL);

        public static native int open(String path, int flags) throws LastErrorException;

        public static native int close(int fd) throws LastErrorException;

        public static native int read(int fd, byte[] buf, int nbytes) throws LastErrorException;

        public static native int write(int fd, byte[] buf, int nbytes) throws LastErrorException;

        public static native int ioctl(int fd, NativeLong request, Structure argp) throws LastErrorException;

        @Structure.FieldOrder({"ifr_name", "ifr_ifru"})
        public static class Ifreq extends Structure {
            public byte[] ifr_name;
            public If.Ifreq.FfrIfru ifr_ifru;

            public Ifreq(final String ifr_name, final short flags) {
                this.ifr_name = new byte[IFNAMSIZ];
                if (ifr_name != null) {
                    final byte[] bytes = ifr_name.getBytes(US_ASCII);
                    System.arraycopy(bytes, 0, this.ifr_name, 0, bytes.length);
                }
                this.ifr_ifru.setType("ifru_flags");
                this.ifr_ifru.ifru_flags = flags;
            }

        }
    }

    private int fd;
    private int mtu = 65535;

    public LinuxTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        fd = LibC.open("/dev/net/tun", LinuxC.O_RDWR);
        If.Ifreq ifreq = new If.Ifreq(getName(), (short) (LinuxC.IFF_TUN | LinuxC.IFF_NO_PI));
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
        byte[] bytes = new byte[mtu];
        int read = LinuxC.read(fd, bytes, bytes.length);
        ByteBuf byteBuf = alloc.buffer(read);
        byteBuf.writeBytes(bytes, 0, read);
        return byteBuf;
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        try {
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            LinuxC.write(fd, bytes, bytes.length);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void close() {
        LinuxC.close(fd);
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

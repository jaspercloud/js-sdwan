package io.jasercloud.sdwan.tun.linux;

import io.jasercloud.sdwan.tun.CheckInvoke;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.ProcessUtil;
import io.jasercloud.sdwan.tun.TunDevice;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.UUID;

public class LinuxTunDevice extends TunDevice {

    private int fd;
    private int mtu = 65535;
    private FdSet fdSet;
    private Timeval timeval;
    private boolean closing = false;

    public LinuxTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        fd = NativeLinuxApi.open("/dev/net/tun", NativeLinuxApi.O_RDWR);
        int flags = NativeLinuxApi.fcntl(fd, NativeLinuxApi.F_GETFL, 0);
        int noblock = NativeLinuxApi.fcntl(fd, NativeLinuxApi.F_SETFL, flags | NativeLinuxApi.O_NONBLOCK);
        CheckInvoke.check(noblock, 0);
        fdSet = new FdSet();
        fdSet.FD_SET(fd);
        timeval = new Timeval();
        timeval.tv_sec = 5;
        Ifreq ifreq = new Ifreq(getName(), (short) (NativeLinuxApi.IFF_TUN | NativeLinuxApi.IFF_NO_PI));
        NativeLinuxApi.ioctl(fd, NativeLinuxApi.TUNSETIFF, ifreq);
        setActive(true);
    }

    @Override
    public int getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        int addAddr = ProcessUtil.exec(String.format("/sbin/ip addr add %s/%s dev %s", addr, netmaskPrefix, getName()));
        CheckInvoke.check(addAddr, 0);
        int up = ProcessUtil.exec(String.format("/sbin/ip link set dev %s up", getName()));
        CheckInvoke.check(up, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        int setMtu = ProcessUtil.exec(String.format("/sbin/ip link set %s mtu %s", getName(), mtu));
        CheckInvoke.check(setMtu, 0);
        this.mtu = mtu;
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            int select = NativeLinuxApi.select(fd + 1, fdSet, null, null, timeval);
            if (-1 == select) {
                throw new ProcessException("select -1");
            }
            if (fdSet.FD_ISSET(fd)) {
                byte[] bytes = new byte[mtu];
                int read = NativeLinuxApi.read(fd, bytes, bytes.length);
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
        NativeLinuxApi.write(fd, bytes, bytes.length);
    }

    @Override
    public void addRoute(String route, String ip) throws Exception {
        String cmd = String.format("ip route add %s via %s", route, ip);
        int addRoute = ProcessUtil.exec(cmd);
        CheckInvoke.check(addRoute, 0);
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        int close = NativeLinuxApi.close(fd);
        CheckInvoke.check(close, 0);
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
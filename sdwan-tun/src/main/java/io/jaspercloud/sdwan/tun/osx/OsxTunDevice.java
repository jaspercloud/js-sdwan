package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class OsxTunDevice extends TunDevice {

    private int fd;
    private int mtu = 65535;
    private boolean closing = false;

    public OsxTunDevice(String tunName, String type, String guid) {
        super(tunName, type, guid);
    }

    @Override
    public void open() throws Exception {
        fd = NativeOsxApi.socket(NativeOsxApi.AF_SYSTEM, NativeOsxApi.SOCK_DGRAM, NativeOsxApi.SYSPROTO_CONTROL);
        CtlInfo ctlInfo = new CtlInfo(NativeOsxApi.UTUN_CONTROL_NAME);
        NativeOsxApi.ioctl(fd, NativeOsxApi.CTLIOCGINFO, ctlInfo);
        SockaddrCtl address = new SockaddrCtl(NativeOsxApi.AF_SYSTEM, (short) NativeOsxApi.SYSPROTO_CONTROL, ctlInfo.ctl_id, 0);
        NativeOsxApi.connect(fd, address, address.sc_len);
        setActive(true);
    }

    @Override
    public int getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        String deviceName = getEthName();
        Ifreq ifreq = new Ifreq(deviceName, mtu);
        NativeOsxApi.ioctl(fd, NativeOsxApi.SIOCSIFMTU, ifreq);
        Cidr cidr = Cidr.parseCidr(String.format("%s/%s", addr, netmaskPrefix));
        int addAddr = ProcessUtil.exec(String.format("ifconfig %s inet %s netmask %s broadcast %s",
                deviceName, addr, cidr.getMaskAddress(), cidr.getBroadcastAddress()));
        CheckInvoke.check(addAddr, 0);
        int up = ProcessUtil.exec(String.format("ifconfig %s up", deviceName));
        CheckInvoke.check(up, 0);
        //route: mac与其他系统的差异
        int route = ProcessUtil.exec(String.format("route -n add -net %s -interface %s", String.format("%s/%s", cidr.getNetworkIdentifier(), netmaskPrefix), deviceName));
        CheckInvoke.check(route, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String deviceName = getEthName();
        Ifreq ifreq = new Ifreq(deviceName, mtu);
        NativeOsxApi.ioctl(fd, NativeOsxApi.SIOCSIFMTU, ifreq);
        this.mtu = mtu;
    }

    public String getEthName() {
        SockName sockName = new SockName();
        IntByReference sockNameLen = new IntByReference(SockName.LENGTH);
        NativeOsxApi.getsockopt(fd, NativeOsxApi.SYSPROTO_CONTROL, NativeOsxApi.UTUN_OPT_IFNAME, sockName, sockNameLen);
        String deviceName = Native.toString(sockName.name, US_ASCII);
        return deviceName;
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            byte[] bytes = new byte[mtu];
            int read = NativeOsxApi.read(fd, bytes, bytes.length);
            if (read <= 0) {
                continue;
            }
            ByteBuf byteBuf = alloc.buffer(read);
            //process loopback
            byteBuf.writeBytes(bytes, 4, read);
            return byteBuf;
        }
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        if (closing) {
            throw new ProcessException("Device is closed.");
        }
        //TunChannel已回收
        byte[] bytes = new byte[msg.readableBytes() + 4];
        bytes[3] = 0x2;
        //process loopback
        msg.readBytes(bytes, 4, msg.readableBytes());
        NativeOsxApi.write(fd, bytes, bytes.length);
    }

    @Override
    public void close() throws Exception {
        if (closing) {
            return;
        }
        closing = true;
        int close = NativeOsxApi.close(fd);
        CheckInvoke.check(close, 0);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

}

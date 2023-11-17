package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class OsxTunDevice extends TunDevice {

    private String ethName;

    private int fd;
    private int mtu = 65535;
    private boolean closing = false;

    public OsxTunDevice(String ethName, String tunName, String type, String guid) {
        super(tunName, type, guid);
        this.ethName = ethName;
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
        int addAddr = ProcessUtil.exec(String.format("ifconfig %s inet %s %s netmask %s",
                deviceName, addr, cidr.getGatewayAddress(), cidr.getMaskAddress()));
        CheckInvoke.check(addAddr, 0);
        int up = ProcessUtil.exec(String.format("ifconfig %s up", deviceName));
        CheckInvoke.check(up, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String deviceName = getEthName();
        Ifreq ifreq = new Ifreq(deviceName, mtu);
        NativeOsxApi.ioctl(fd, NativeOsxApi.SIOCSIFMTU, ifreq);
        this.mtu = mtu;
    }

    private String getEthName() {
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
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        NativeOsxApi.write(fd, bytes, bytes.length);
    }

    @Override
    public void addRoute(NetworkInterfaceInfo interfaceInfo, String route, String ip) throws Exception {
        String deviceName = getEthName();
        {
            String cmd = String.format("route -n delete -net %s -interface %s", route, deviceName);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0, 2);
        }
        {
            String cmd = String.format("route -n add -net %s -interface %s", route, deviceName);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
    }

    @Override
    public void delRoute(NetworkInterfaceInfo interfaceInfo, String route, String ip) throws Exception {
        String deviceName = getEthName();
        String cmd = String.format("route -n delete -net %s -interface %s", route, deviceName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0, 2);
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

package io.jaspercloud.sdwan.tun.linux;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class LinuxTunDevice extends TunDevice {

    private String ethName;
    private int fd;
    private int mtu = 65535;
    private Timeval timeval;
    private boolean closing = false;

    public LinuxTunDevice(String tunName, String ethName, String type, String guid) {
        super(tunName, type, guid);
        this.ethName = ethName;
    }

    @Override
    public void open() throws Exception {
        fd = NativeLinuxApi.open("/dev/net/tun", NativeLinuxApi.O_RDWR);
        int flags = NativeLinuxApi.fcntl(fd, NativeLinuxApi.F_GETFL, 0);
        int noblock = NativeLinuxApi.fcntl(fd, NativeLinuxApi.F_SETFL, flags | NativeLinuxApi.O_NONBLOCK);
        CheckInvoke.check(noblock, 0);
        timeval = new Timeval();
        timeval.tv_sec = 5;
        Ifreq ifreq = new Ifreq(getName(), (short) (NativeLinuxApi.IFF_TUN | NativeLinuxApi.IFF_NO_PI));
        NativeLinuxApi.ioctl(fd, NativeLinuxApi.TUNSETIFF, ifreq);
        enableIpForward();
        addIptablesRules();
        setActive(true);
    }

    private void enableIpForward() {
        File file = new File("/proc/sys/net/ipv4/ip_forward");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))) {
            FileCopyUtils.copy("1", writer);
        } catch (Throwable e) {
            throw new ProcessException("enableIpForward failed", e);
        }
    }

    private void addIptablesRules() throws IOException, InterruptedException {
        //filter
        if (Iptables.queryFilterRule(getName(), ethName)) {
            Iptables.deleteFilterRule(getName(), ethName);
        }
        Iptables.addFilterRule(getName(), ethName);
        //nat
        if (Iptables.queryNatRule(ethName)) {
            Iptables.deleteNatRule(ethName);
        }
        Iptables.addNatRule(ethName);
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
            FdSet fdSet = new FdSet();
            fdSet.FD_SET(fd);
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
        //TunChannel已回收
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        NativeLinuxApi.write(fd, bytes, bytes.length);
    }

    @Override
    public void close() throws Exception {
        if (closing) {
            return;
        }
        closing = true;
        int close = NativeLinuxApi.close(fd);
        CheckInvoke.check(close, 0);
        Iptables.deleteFilterRule(getName(), ethName);
        Iptables.deleteNatRule(ethName);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

}

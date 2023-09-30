package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.linux.LinuxTunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

public class LinuxTunTest {

    public static void main(String[] args) throws Exception {
        int fd = LinuxTunDevice.LinuxC.open("/dev/net/tun", LinuxTunDevice.LinuxC.O_RDWR);
        int flags = LinuxTunDevice.LinuxC.fcntl(fd, LinuxTunDevice.LinuxC.F_GETFL, 0);
        int fcntl = LinuxTunDevice.LinuxC.fcntl(fd, LinuxTunDevice.LinuxC.F_SETFL, flags | LinuxTunDevice.LinuxC.O_NONBLOCK);
        LinuxTunDevice.LinuxC.Ifreq ifreq = new LinuxTunDevice.LinuxC.Ifreq("tun", (short) (LinuxTunDevice.LinuxC.IFF_TUN | LinuxTunDevice.LinuxC.IFF_NO_PI));
        LinuxTunDevice.LinuxC.ioctl(fd, LinuxTunDevice.LinuxC.TUNSETIFF, ifreq);
        int addAddr = ProcessUtil.exec(String.format("/sbin/ip addr add %s/%s dev %s", "192.168.1.1", 20, "tun"));
        int up = ProcessUtil.exec(String.format("/sbin/ip link set dev %s up", "tun"));
        new Thread(() -> {
            try {
                Thread.sleep(15 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            int close = LinuxTunDevice.LinuxC.close(fd);
            System.out.println("close:" + close);
        }).start();
        while (true) {
            LinuxTunDevice.LinuxC.FdSet fdSet = new LinuxTunDevice.LinuxC.FdSet();
            fdSet.FD_SET(fd);
            LinuxTunDevice.LinuxC.Timeval timeval = new LinuxTunDevice.LinuxC.Timeval();
            timeval.tv_sec = 5;
            int select = LinuxTunDevice.LinuxC.select(fd + 1, fdSet, null, null, timeval);
            if (-1 == select) {
                System.out.println();
                return;
            }
            if (fdSet.FD_ISSET(fd)) {
                try {
                    byte[] tmp = new byte[1500];
                    int read = LinuxTunDevice.LinuxC.read(fd, tmp, tmp.length);
                    System.out.println("read:" + read);
                    byte[] bytes = Arrays.copyOf(tmp, read);
                    int version = (bytes[0] >> 4);
                    if (4 == version) {
                        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                        Ipv4Packet decode = Ipv4Packet.decode(byteBuf);
                        System.out.println(String.format("%s -> %s", decode.getSrcIP(), decode.getDstIP()));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

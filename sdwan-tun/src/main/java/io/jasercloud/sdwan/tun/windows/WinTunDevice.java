package io.jasercloud.sdwan.tun.windows;

import com.sun.jna.*;
import io.jasercloud.sdwan.tun.CheckInvoke;
import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.ProcessUtil;
import io.jasercloud.sdwan.tun.TunDevice;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.UUID;

public class WinTunDevice extends TunDevice {

    private Pointer adapter;
    private Pointer session;
    private boolean closing = false;

    public WinTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        adapter = NativeWinTunApi.WintunCreateAdapter(new WString(getName()), new WString(getType()), getGuid());
        session = NativeWinTunApi.WintunStartSession(adapter, NativeWinTunApi.WINTUN_MAX_RING_CAPACITY);
        setActive(true);
    }

    @Override
    public int getVersion() {
        return NativeWinTunApi.WintunGetRunningDriverVersion();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        String cmd = String.format("netsh interface ipv4 set address name=\"%s\" static %s/%s", getName(), addr, netmaskPrefix);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String cmd = String.format("netsh interface ipv4 set subinterface \"%s\" mtu=%s store=active", getName(), mtu);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            try {
                Pointer packetSizePointer = new Memory(Native.POINTER_SIZE);
                Pointer packetPointer = NativeWinTunApi.WintunReceivePacket(session, packetSizePointer);
                try {
                    int packetSize = packetSizePointer.getInt(0);
                    byte[] bytes = packetPointer.getByteArray(0, packetSize);
                    ByteBuf byteBuf = alloc.buffer(bytes.length);
                    byteBuf.writeBytes(bytes);
                    return byteBuf;
                } finally {
                    NativeWinTunApi.WintunReleaseReceivePacket(session, packetPointer);
                }
            } catch (LastErrorException e) {
                if (e.getErrorCode() == NativeWinTunApi.ERROR_NO_MORE_ITEMS) {
                    NativeKernel32Api.INSTANCE.WaitForSingleObject(NativeWinTunApi.WintunGetReadWaitEvent(session), NativeKernel32Api.INFINITE);
                } else {
                    throw e;
                }
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
        Pointer packetPointer = NativeWinTunApi.WintunAllocateSendPacket(session, bytes.length);
        packetPointer.write(0, bytes, 0, bytes.length);
        NativeWinTunApi.WintunSendPacket(session, packetPointer);
    }

    @Override
    public void addRoute(NetworkInterfaceInfo interfaceInfo, String route, String ip) throws Exception {
        {
            String cmd = String.format("route delete %s %s", route, ip);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
        {
            String cmd = String.format("route add %s %s if %s", route, ip, interfaceInfo.getIndex());
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        NativeWinTunApi.WintunEndSession(session);
        NativeWinTunApi.WintunCloseAdapter(adapter);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

    public static void main(String[] args) throws Exception {
        WinTunDevice tunDevice = new WinTunDevice("tun", "sdwan", UUID.randomUUID().toString());
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

package io.jaspercloud.sdwan.tun.windows;

import com.sun.jna.*;
import com.sun.jna.win32.StdCallLibrary;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

import java.util.UUID;

public class WinTunDevice extends TunDevice {

    private static class WinTunApi {

        static {
            try {
                Native.register(WinTunApi.class, "wintun");
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public static final int ERROR_NO_MORE_ITEMS = 259;
        public static final int WINTUN_MIN_RING_CAPACITY = 0x20000;
        public static final int WINTUN_MAX_RING_CAPACITY = 0x4000000;

        public static native int WintunGetRunningDriverVersion() throws LastErrorException;

        public static native Pointer WintunCreateAdapter(WString Name, WString TunnelType, String RequestedGUID) throws LastErrorException;

        public static native void WintunCloseAdapter(Pointer Adapter) throws LastErrorException;

        public static native Pointer WintunStartSession(Pointer Adapter, int Capacity) throws LastErrorException;

        public static native void WintunEndSession(Pointer Session) throws LastErrorException;

        public static native Pointer WintunReceivePacket(Pointer Session, Pointer PacketSize) throws LastErrorException;

        public static native void WintunReleaseReceivePacket(Pointer Session, Pointer Packet) throws LastErrorException;

        public static native Pointer WintunAllocateSendPacket(Pointer Session, long PacketSize) throws LastErrorException;

        public static native void WintunSendPacket(Pointer Session, Pointer Packet) throws LastErrorException;

        public static native Pointer WintunGetReadWaitEvent(Pointer Session) throws LastErrorException;
    }

    private interface Kernel32 extends StdCallLibrary {

        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        int INFINITE = 0xFFFFFFFF;

        int WaitForSingleObject(Pointer hHandle, int dwMilliseconds);
    }

    private Pointer adapter;
    private Pointer session;

    public WinTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        adapter = WinTunApi.WintunCreateAdapter(new WString(getName()), new WString(getType()), getGuid());
        session = WinTunApi.WintunStartSession(adapter, WinTunApi.WINTUN_MAX_RING_CAPACITY);
        setActive(true);
    }

    @Override
    public int getVersion() {
        return WinTunApi.WintunGetRunningDriverVersion();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        String cmd = String.format("netsh interface ipv4 set address name=\"%s\" static %s/%s", getName(), addr, netmaskPrefix);
        int code = ProcessUtil.exec(cmd);
        if (0 != code) {
        }
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String cmd = String.format("netsh interface ipv4 set subinterface \"%s\" mtu=%s store=active", getName(), mtu);
        int code = ProcessUtil.exec(cmd);
        if (0 != code) {
        }
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            try {
                Pointer packetSizePointer = new Memory(Native.POINTER_SIZE);
                Pointer packetPointer = WinTunApi.WintunReceivePacket(session, packetSizePointer);
                try {
                    int packetSize = packetSizePointer.getInt(0);
                    byte[] bytes = packetPointer.getByteArray(0, packetSize);
                    ByteBuf byteBuf = alloc.buffer(bytes.length);
                    byteBuf.writeBytes(bytes);
                    return byteBuf;
                } finally {
                    WinTunApi.WintunReleaseReceivePacket(session, packetPointer);
                }
            } catch (LastErrorException e) {
                if (e.getErrorCode() == WinTunApi.ERROR_NO_MORE_ITEMS) {
                    Kernel32.INSTANCE.WaitForSingleObject(WinTunApi.WintunGetReadWaitEvent(session), Kernel32.INFINITE);
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        try {
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            Pointer packetPointer = WinTunApi.WintunAllocateSendPacket(session, bytes.length);
            packetPointer.write(0, bytes, 0, bytes.length);
            WinTunApi.WintunSendPacket(session, packetPointer);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void addRoute(String route, String ip) throws Exception {
        String cmd = String.format("route add %s %s", route, ip);
        int addRoute = ProcessUtil.exec(cmd);
    }

    @Override
    public void close() {
        WinTunApi.WintunEndSession(session);
        WinTunApi.WintunCloseAdapter(adapter);
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

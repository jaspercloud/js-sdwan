package io.jaspercloud.sdwan.tun.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public class NativeWinTunApi {

    static {
        try {
            Native.register(NativeWinTunApi.class, "wintun");
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

    public static native void WintunGetAdapterLUID(Pointer Adapter, Pointer Luid) throws LastErrorException;
}

/*
 * Copyright (c) 2021-2022 Heiko Bornholdt and Kevin Röbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.drasyl.channel.tun.jna.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import io.netty.util.internal.SystemPropertyUtil;
import org.drasyl.channel.tun.jna.windows.Guid.GUID;
import org.drasyl.channel.tun.jna.windows.WinDef.DWORD;
import org.drasyl.channel.tun.jna.windows.WinNT.HANDLE;
import org.drasyl.channel.tun.jna.windows.loader.LibraryLoader;

import java.io.IOException;

import static org.drasyl.channel.tun.jna.windows.loader.LibraryLoader.PREFER_SYSTEM;

/**
 * JNA mapping for the <a href="https://www.wintun.net/">Wintun Network Adapter</a>.
 */
@SuppressWarnings({ "java:S100", "java:S117" })
public final class Wintun {
    private static final String DEFAULT_MODE = SystemPropertyUtil.get("tun.native.mode", PREFER_SYSTEM);

    static {
        try {
            new LibraryLoader(Wintun.class).loadLibrary(DEFAULT_MODE, "wintun");
        }
        catch (final IOException e) {
            throw new RuntimeException(e); // NOSONAR
        }
    }

    private Wintun() {
        // JNA mapping
    }

    /**
     * Creates a new Wintun adapter.
     *
     * @param Name          The requested name of the adapter. Zero-terminated string of up to
     *                      MAX_ADAPTER_NAME-1 characters.
     * @param TunnelType    Name of the adapter tunnel type. Zero-terminated string of up to
     *                      MAX_ADAPTER_NAME-1 characters.
     * @param RequestedGUID The GUID of the created network adapter, which then influences NLA
     *                      generation deterministically. If it is set to NULL, the GUID is chosen
     *                      by the system at random, and hence a new NLA entry is created for each
     *                      new adapter. It is called "requested" GUID because the API it uses is
     *                      completely undocumented, and so there could be minor interesting
     *                      complications with its usage.
     * @return If the function succeeds, the return value is the adapter handle. Must be released
     * with WintunCloseAdapter. If the function fails, the return value is NULL. To get extended
     * error information, call GetLastError.
     * @throws LastErrorException
     */
    public static native WINTUN_ADAPTER_HANDLE WintunCreateAdapter(final WString Name,
                                                                   final WString TunnelType,
                                                                   final GUID RequestedGUID) throws LastErrorException;

    /**
     * Releases Wintun adapter resources and, if adapter was created with WintunCreateAdapter,
     * removes adapter.
     *
     * @param Adapter Adapter handle obtained with WintunCreateAdapter or WintunOpenAdapter.
     * @throws LastErrorException
     */
    public static native void WintunCloseAdapter(final WINTUN_ADAPTER_HANDLE Adapter) throws LastErrorException;

    /**
     * Returns the LUID of the adapter.
     *
     * @param Adapter Adapter handle obtained with WintunOpenAdapter or WintunCreateAdapter.
     * @param Luid    Pointer to LUID to receive adapter LUID.
     * @throws LastErrorException
     */
    public static native void WintunGetAdapterLUID(final WINTUN_ADAPTER_HANDLE Adapter,
                                                   final Pointer Luid) throws LastErrorException;

    /**
     * Determines the version of the Wintun driver currently loaded.
     *
     * @return If the function succeeds, the return value is the version number. If the function
     * fails, the return value is zero. To get extended error information, call GetLastError.
     * Possible errors include the following: ERROR_FILE_NOT_FOUND Wintun not loaded
     * @throws LastErrorException
     */
    public static native DWORD WintunGetRunningDriverVersion() throws LastErrorException;

    /**
     * Starts Wintun session.
     *
     * @param Adapter  Adapter handle obtained with WintunOpenAdapter or WintunCreateAdapter.
     * @param Capacity Rings capacity. Must be between WINTUN_MIN_RING_CAPACITY and
     *                 WINTUN_MAX_RING_CAPACITY (incl.) Must be a power of two.
     * @return Wintun session handle. Must be released with WintunEndSession. If the function fails,
     * the return value is NULL. To get extended error information, call GetLastError.
     * @throws LastErrorException
     */
    public static native WINTUN_SESSION_HANDLE WintunStartSession(final WINTUN_ADAPTER_HANDLE Adapter,
                                                                  final DWORD Capacity) throws LastErrorException;

    /**
     * Ends Wintun session.
     *
     * @param Session Wintun session handle obtained with WintunStartSession.
     * @throws LastErrorException
     */
    public static native void WintunEndSession(final WINTUN_SESSION_HANDLE Session) throws LastErrorException;

    /**
     * Gets Wintun session's read-wait event handle.
     *
     * @param Session Wintun session handle obtained with WintunStartSession.
     * @return Pointer to receive event handle to wait for available data when reading. Should
     * WintunReceivePackets return ERROR_NO_MORE_ITEMS (after spinning on it for a while under heavy
     * load), wait for this event to become signaled before retrying WintunReceivePackets. Do not
     * call CloseHandle on this event - it is managed by the session.
     * @throws LastErrorException
     */
    public static native HANDLE WintunGetReadWaitEvent(WINTUN_SESSION_HANDLE Session) throws LastErrorException;

    /**
     * Retrieves one or packet. After the packet content is consumed, call
     * WintunReleaseReceivePacket with Packet returned from this function to release internal
     * buffer. This function is thread-safe.
     *
     * @param Session    Wintun session handle obtained with WintunStartSession.
     * @param PacketSize Pointer to receive packet size.
     * @return Pointer to layer 3 IPv4 or IPv6 packet. Client may modify its content at will. If the
     * function fails, the return value is NULL. To get extended error information, call
     * GetLastError. Possible errors include the following: ERROR_HANDLE_EOF Wintun adapter is
     * terminating; ERROR_NO_MORE_ITEMS Wintun buffer is exhausted; ERROR_INVALID_DATA Wintun buffer
     * is corrupt.
     * @throws LastErrorException
     */
    public static native Pointer WintunReceivePacket(final WINTUN_SESSION_HANDLE Session,
                                                     final Pointer PacketSize) throws LastErrorException;

    /**
     * Releases internal buffer after the received packet has been processed by the client. This
     * function is thread-safe.
     *
     * @param Session Wintun session handle obtained with WintunStartSession.
     * @param Packet  Packet obtained with WintunReceivePacket.
     * @throws LastErrorException
     */
    public static native void WintunReleaseReceivePacket(final WINTUN_SESSION_HANDLE Session,
                                                         final Pointer Packet) throws LastErrorException;

    /**
     * Allocates memory for a packet to send. After the memory is filled with packet data, call
     * WintunSendPacket to send and release internal buffer. WintunAllocateSendPacket is thread-safe
     * and the WintunAllocateSendPacket order of calls define the packet sending order.
     *
     * @param Session    Wintun session handle obtained with WintunStartSession.
     * @param PacketSize Exact packet size. Must be less or equal to WINTUN_MAX_IP_PACKET_SIZE.
     * @return
     * @throws LastErrorException
     */
    public static native Pointer WintunAllocateSendPacket(final WINTUN_SESSION_HANDLE Session,
                                                          final DWORD PacketSize) throws LastErrorException;

    /**
     * Sends the packet and releases internal buffer. WintunSendPacket is thread-safe, but the
     * WintunAllocateSendPacket order of calls define the packet sending order. This means the
     * packet is not guaranteed to be sent in the WintunSendPacket yet.
     *
     * @param Session Wintun session handle obtained with WintunStartSession.
     * @param Packet  Packet obtained with WintunAllocateSendPacket.
     * @throws LastErrorException
     */
    public static native void WintunSendPacket(WINTUN_SESSION_HANDLE Session,
                                               final Pointer Packet) throws LastErrorException;

    /**
     * A handle representing Wintun adapter.
     */
    @SuppressWarnings("java:S101")
    public static class WINTUN_ADAPTER_HANDLE extends HANDLE {
    }

    /**
     * A handle representing Wintun session.
     */
    @SuppressWarnings("java:S101")
    public static class WINTUN_SESSION_HANDLE extends HANDLE {
    }
}

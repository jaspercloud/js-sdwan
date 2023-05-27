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

import com.sun.jna.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import org.drasyl.channel.tun.Tun4Packet;
import org.drasyl.channel.tun.Tun6Packet;
import org.drasyl.channel.tun.TunAddress;
import org.drasyl.channel.tun.TunPacket;
import org.drasyl.channel.tun.jna.AbstractTunDevice;
import org.drasyl.channel.tun.jna.TunDevice;
import org.drasyl.channel.tun.jna.windows.Guid.GUID;
import org.drasyl.channel.tun.jna.windows.WinDef.DWORD;
import org.drasyl.channel.tun.jna.windows.Wintun.*;

import java.io.IOException;

import static java.util.Objects.requireNonNull;
import static org.drasyl.channel.tun.jna.windows.WinBase.INFINITE;
import static org.drasyl.channel.tun.jna.windows.WinError.ERROR_NO_MORE_ITEMS;
import static org.drasyl.channel.tun.jna.windows.Wintun.*;

/**
 * {@link TunDevice} implementation for Windows-based platforms.
 */
public final class WindowsTunDevice extends AbstractTunDevice {
    public static final WString TUNNEL_TYPE = new WString("drasyl");
    private final WINTUN_ADAPTER_HANDLE adapter;
    private final WINTUN_SESSION_HANDLE session;

    private WindowsTunDevice(final WINTUN_ADAPTER_HANDLE adapter,
                             final WINTUN_SESSION_HANDLE session,
                             final TunAddress localAddress) {
        super(localAddress);
        this.adapter = requireNonNull(adapter);
        this.session = requireNonNull(session);
    }

    public static TunDevice open(String name) throws IOException {
        if (name == null) {
            name = "tun";
        }

        WINTUN_ADAPTER_HANDLE adapter = null;
        WINTUN_SESSION_HANDLE session = null;
        try {
            adapter = WintunCreateAdapter(new WString(name), TUNNEL_TYPE, GUID.newGuid());
            DWORD version = WintunGetRunningDriverVersion();
            System.out.println(String.format("wintun version: %s", version.intValue()));
            session = WintunStartSession(adapter, new DWORD(0x400000));
        }
        catch (final LastErrorException e) {
            if (session != null) {
                WintunEndSession(session);
            }

            if (adapter != null) {
                WintunCloseAdapter(adapter);
            }

            throw new IOException(e);
        }

        return new WindowsTunDevice(adapter, session, new TunAddress(name));
    }

    @SuppressWarnings("java:S109")
    @Override
    public TunPacket readPacket(final ByteBufAllocator alloc) throws IOException {
        if (closed) {
            throw new IOException("Device is closed.");
        }

        while (true) {
            try {
                // read bytes
                final Pointer packetSizePointer = new Memory(Native.POINTER_SIZE);
                final Pointer packetPointer = WintunReceivePacket(session, packetSizePointer);

                // extract ip version
                final int ipVersion = packetPointer.getByte(0) >> 4;

                // shrink bytebuf to actual required size
                final int PacketSize = packetSizePointer.getInt(0);
                final ByteBuf byteBuf = alloc.buffer(PacketSize);
                byteBuf.writeBytes(packetPointer.getByteArray(0, PacketSize));
                WintunReleaseReceivePacket(session, packetPointer);

                if (ipVersion == 4) {
                    return new Tun4Packet(byteBuf);
                }
                else {
                    return new Tun6Packet(byteBuf);
                }
            }
            catch (final LastErrorException e) {
                if (e.getErrorCode() == ERROR_NO_MORE_ITEMS) {
                    Kernel32.INSTANCE.WaitForSingleObject(WintunGetReadWaitEvent(session), INFINITE);
                }
                else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void writePacket(final ByteBufAllocator alloc, final TunPacket msg) throws IOException {
        if (closed) {
            throw new IOException("Device is closed.");
        }

        final DWORD packetSize = new DWORD(msg.content().readableBytes());
        final Pointer packetPointer = WintunAllocateSendPacket(session, packetSize);
        packetPointer.write(0, ByteBufUtil.getBytes(msg.content()), 0, msg.content().readableBytes());

        WintunSendPacket(session, packetPointer);

        msg.release();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            if (session != null) {
                WintunEndSession(session);
            }
            if (adapter != null) {
                WintunCloseAdapter(adapter);
            }
        }
    }

    public WINTUN_ADAPTER_HANDLE adapter() {
        return adapter;
    }
}

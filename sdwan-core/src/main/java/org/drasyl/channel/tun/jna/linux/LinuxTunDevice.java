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
package org.drasyl.channel.tun.jna.linux;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.drasyl.channel.tun.Tun4Packet;
import org.drasyl.channel.tun.Tun6Packet;
import org.drasyl.channel.tun.TunAddress;
import org.drasyl.channel.tun.TunPacket;
import org.drasyl.channel.tun.jna.AbstractTunDevice;
import org.drasyl.channel.tun.jna.TunDevice;
import org.drasyl.channel.tun.jna.shared.If.Ifreq;
import org.drasyl.channel.tun.jna.shared.LibC;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.drasyl.channel.tun.jna.linux.Fcntl.O_RDWR;
import static org.drasyl.channel.tun.jna.linux.IfTun.*;
import static org.drasyl.channel.tun.jna.linux.Sockios.SIOCGIFMTU;
import static org.drasyl.channel.tun.jna.linux.Sockios.SIOCSIFMTU;
import static org.drasyl.channel.tun.jna.shared.If.IFNAMSIZ;
import static org.drasyl.channel.tun.jna.shared.LibC.*;
import static org.drasyl.channel.tun.jna.shared.Socket.AF_INET;
import static org.drasyl.channel.tun.jna.shared.Socket.SOCK_DGRAM;

/**
 * {@link TunDevice} implementation for Linux-based platforms.
 */
public final class LinuxTunDevice extends AbstractTunDevice {
    private static final IllegalArgumentException ILLEGAL_NAME_EXCEPTION = new IllegalArgumentException("Device name must be an ASCII string shorter than 16 characters or null.");
    private final int fd;
    private final NativeLong mtu;

    private LinuxTunDevice(final int fd, final int mtu, final TunAddress localAddress) {
        super(localAddress);
        this.fd = fd;
        this.mtu = new NativeLong(mtu);
    }

    public static TunDevice open(String name, int mtu) throws IOException {
        if (name != null && name.isEmpty()) {
            name = null;
        }
        if (name != null && (name.length() >= IFNAMSIZ || !US_ASCII.newEncoder().canEncode(name))) {
            throw ILLEGAL_NAME_EXCEPTION;
        }

        // open tun device
        final int fd = LibC.open("/dev/net/tun", O_RDWR);

        if (fd == -1) {
            throw new IOException("Create an endpoint for communication failed.");
        }

        // configure/create actual tun device
        final Ifreq ifreq = new Ifreq(name, (short) (IFF_TUN | IFF_NO_PI));
        ioctl(fd, TUNSETIFF, ifreq);

        final String deviceName = Native.toString(ifreq.ifr_name, US_ASCII);

        final int s = socket(AF_INET, SOCK_DGRAM, 0);
        if (mtu != 0) {
            // set mtu
            final Ifreq ifreq2 = new Ifreq(deviceName, mtu);
            ioctl(s, SIOCSIFMTU, ifreq2);
            mtu = ifreq2.ifr_ifru.ifru_mtu;
        }
        else {
            // get mtu
            final Ifreq ifreq2 = new Ifreq(deviceName);
            ioctl(s, SIOCGIFMTU, ifreq2);
            mtu = ifreq2.ifr_ifru.ifru_mtu;
        }

        return new LinuxTunDevice(fd, mtu, new TunAddress(deviceName));
    }

    @SuppressWarnings("java:S109")
    @Override
    public TunPacket readPacket(final ByteBufAllocator alloc) throws IOException {
        if (closed) {
            throw new IOException("Device is closed.");
        }

        // read from socket
        final int capacity = mtu.intValue();
        final ByteBuf maxByteBuf = alloc.buffer(capacity).writerIndex(capacity);
        final ByteBuffer byteBuffer = maxByteBuf.nioBuffer();
        final int bytesRead = read(fd, byteBuffer, mtu);

        // shrink bytebuf to actual required size
        final ByteBuf actualByteBuf = maxByteBuf
                .writerIndex(bytesRead)
                .capacity(bytesRead)
                .slice();

        // extract ip version
        final int version = Byte.toUnsignedInt(actualByteBuf.getByte(0)) >> 4;

        if (version == 4) {
            return new Tun4Packet(actualByteBuf);
        }
        else if (version == 6) {
            return new Tun6Packet(actualByteBuf);
        }
        else {
            throw new IOException("Unknown protocol: " + version);
        }
    }

    @Override
    public void writePacket(final ByteBufAllocator alloc, final TunPacket msg) throws IOException {
        if (closed) {
            throw new IOException("Device is closed.");
        }

        // write to socket
        final ByteBuffer byteBuffer = msg.content().nioBuffer();
        write(fd, byteBuffer, new NativeLong(byteBuffer.remaining()));

        msg.release();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            // close tun device
            LibC.close(fd);
        }
    }
}

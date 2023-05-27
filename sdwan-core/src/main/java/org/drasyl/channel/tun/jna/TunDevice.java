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
package org.drasyl.channel.tun.jna;

import io.netty.buffer.ByteBufAllocator;
import org.drasyl.channel.tun.TunAddress;
import org.drasyl.channel.tun.TunPacket;

import java.io.Closeable;
import java.io.IOException;

/**
 * Defines a TUN device we can read from/write to.
 */
public interface TunDevice extends Closeable {
    /**
     * The actual name of the device.
     *
     * @return The actual name of the device.
     */
    TunAddress localAddress();

    /**
     * Reads and blocks until a {@link TunPacket} has been received by the tun device.
     *
     * @param alloc
     * @return {@link TunPacket} received by the tun device
     * @throws IOException if read failed
     */
    TunPacket readPacket(final ByteBufAllocator alloc) throws IOException;

    /**
     * Writes and blocks until a {@link TunPacket} has been sent by the tun device.
     *
     * @param alloc
     * @param msg   {@link TunPacket} to write by the tun device
     * @throws IOException if write failed
     */
    void writePacket(final ByteBufAllocator alloc, final TunPacket msg) throws IOException;

    /**
     * Returns whether the device is closed or not.
     *
     * @return {@code true} if the device has been closed.
     */
    boolean isClosed();
}

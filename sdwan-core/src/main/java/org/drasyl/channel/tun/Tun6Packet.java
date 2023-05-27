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
package org.drasyl.channel.tun;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IPv6-based {@link TunPacket}.
 */
public class Tun6Packet extends TunPacket {
    public static final int INET6_HEADER_LENGTH = 40;
    // https://datatracker.ietf.org/doc/html/rfc2460#section-3
    public static final int INET6_VERSION_AND_TRAFFIC_CLASS = 0;
    public static final int INET6_FLOW_LABEL = 1;
    public static final int INET6_PAYLOAD_LENGTH = 4;
    public static final int INET6_NEXT_HEADER = 6;
    public static final int INET6_HOP_LIMIT = 7;
    public static final int INET6_SOURCE_ADDRESS = 8;
    public static final int INET6_SOURCE_ADDRESS_LENGTH = 16;
    public static final int INET6_DESTINATION_ADDRESS = 24;
    public static final int INET6_DESTINATION_ADDRESS_LENGTH = 16;
    private InetAddress sourceAddress;
    private InetAddress destinationAddress;

    public Tun6Packet(final ByteBuf data) {
        super(data);
    }

    @SuppressWarnings("java:S109")
    @Override
    public int version() {
        return content().getUnsignedByte(INET6_VERSION_AND_TRAFFIC_CLASS) >> 4;
    }

    public int trafficClass() {
        return content().getUnsignedShort(INET6_VERSION_AND_TRAFFIC_CLASS) >> 4 & 0x0f;
    }

    public long flowLabel() {
        return content().getUnsignedInt(INET6_FLOW_LABEL) >> 8 & 0x0fffff;
    }

    public long payloadLength() {
        return content().getUnsignedShort(INET6_PAYLOAD_LENGTH);
    }

    public int nextHeader() {
        return content().getUnsignedByte(INET6_NEXT_HEADER);
    }

    public int hopLimit() {
        return content().getUnsignedByte(INET6_HOP_LIMIT);
    }

    @SuppressWarnings("java:S1166")
    @Override
    public InetAddress sourceAddress() {
        if (sourceAddress == null) {
            try {
                byte[] dst = new byte[INET6_SOURCE_ADDRESS_LENGTH];
                content().getBytes(INET6_SOURCE_ADDRESS, dst, 0, INET6_SOURCE_ADDRESS_LENGTH);
                sourceAddress = InetAddress.getByAddress(dst);
            }
            catch (final UnknownHostException e) {
                // unreachable code
                throw new IllegalStateException();
            }
        }
        return sourceAddress;
    }

    @SuppressWarnings("java:S1166")
    @Override
    public InetAddress destinationAddress() {
        if (destinationAddress == null) {
            try {
                byte[] dst = new byte[INET6_SOURCE_ADDRESS_LENGTH];
                content().getBytes(INET6_DESTINATION_ADDRESS, dst, 0, INET6_DESTINATION_ADDRESS_LENGTH);
                destinationAddress = InetAddress.getByAddress(dst);
            }
            catch (final UnknownHostException e) {
                // unreachable code
                throw new IllegalStateException();
            }
        }
        return destinationAddress;
    }

    public byte[] data() {
        final byte[] data = new byte[content().readableBytes() - INET6_HEADER_LENGTH];
        content().getBytes(INET6_HEADER_LENGTH, data);
        return data;
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
                .append('[')
                .append("len=").append(payloadLength())
                .append(", src=").append(sourceAddress().getHostAddress())
                .append(", dst=").append(destinationAddress().getHostAddress())
                .append(']').toString();
    }
}

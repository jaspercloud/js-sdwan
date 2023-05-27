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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IPv4-based {@link TunPacket}.
 */
@SuppressWarnings("unused")
public class Tun4Packet extends TunPacket {
    public static final int INET4_HEADER_LENGTH = 20;
    // https://datatracker.ietf.org/doc/html/rfc791#section-3.1
    public static final int INET4_VERSION_AND_INTERNET_HEADER_LENGTH = 0;
    public static final int INET4_TYPE_OF_SERVICE = 1;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_ROUTINE = 0;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_PRIORITY = 1;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_IMMEDIATE = 2;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_FLASH = 3;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_FLASH_OVERRIDE = 4;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_CRITIC_ECP = 5;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_INTERNETWORK_CONTROL = 6;
    public static final int INET4_TYPE_OF_SERVICE_PRECEDENCE_NETWORK_CONTROL = 7;
    public static final int INET4_TYPE_OF_SERVICE_DELAY_MASK = 1 << 3;
    public static final int INET4_TYPE_OF_SERVICE_THROUGHPUT_MASK = 1 << 4;
    public static final int INET4_TYPE_OF_SERVICE_RELIBILITY_MASK = 1 << 5;
    public static final int INET4_TOTAL_LENGTH = 2;
    public static final int INET4_IDENTIFICATION = 4;
    public static final int INET4_FLAGS_AND_FRAGMENT_OFFSET = 6;
    public static final int INET4_FLAGS_DONT_FRAGMENT_MASK = 1 << 1;
    public static final int INET4_FLAGS_MORE_FRAGMENTS_MASK = 1 << 2;
    public static final int INET4_TIME_TO_LIVE = 8;
    public static final int INET4_PROTOCOL = 9;
    public static final int INET4_HEADER_CHECKSUM = 10;
    public static final int INET4_SOURCE_ADDRESS = 12;
    public static final int INET4_SOURCE_ADDRESS_LENGTH = 4;
    public static final int INET4_DESTINATION_ADDRESS = 16;
    public static final int INET4_DESTINATION_ADDRESS_LENGTH = 4;
    private InetAddress sourceAddress;
    private InetAddress destinationAddress;

    public Tun4Packet(final ByteBuf data) {
        super(data);
        if (data.readableBytes() < INET4_HEADER_LENGTH) {
            throw new IllegalArgumentException("data has only " + data.readableBytes() + " readable bytes. But an IPv4 packet must be at least " + INET4_HEADER_LENGTH + " bytes long.");
        }
    }

    @Override
    public int version() {
        return content().getUnsignedByte(INET4_VERSION_AND_INTERNET_HEADER_LENGTH) >> 4;
    }

    public int internetHeaderLength() {
        return content().getUnsignedByte(INET4_VERSION_AND_INTERNET_HEADER_LENGTH) & 0x0f;
    }

    public int typeOfService() {
        return content().getUnsignedShort(INET4_TYPE_OF_SERVICE);
    }

    public int totalLength() {
        return content().getUnsignedShort(INET4_TOTAL_LENGTH);
    }

    public int identification() {
        return content().getUnsignedShort(INET4_IDENTIFICATION);
    }

    public int flags() {
        return content().getUnsignedByte(INET4_FLAGS_AND_FRAGMENT_OFFSET) >> 5;
    }

    public int fragmentOffset() {
        return content().getUnsignedShort(INET4_FLAGS_AND_FRAGMENT_OFFSET) & 0x01fff;
    }

    public int timeToLive() {
        return content().getUnsignedByte(INET4_TIME_TO_LIVE);
    }

    public int protocol() {
        return content().getUnsignedByte(INET4_PROTOCOL);
    }

    public int headerChecksum() {
        return content().getUnsignedShort(INET4_HEADER_CHECKSUM);
    }

    @SuppressWarnings("java:S1166")
    @Override
    public InetAddress sourceAddress() {
        if (sourceAddress == null) {
            try {
                byte[] dst = new byte[INET4_SOURCE_ADDRESS_LENGTH];
                content().getBytes(INET4_SOURCE_ADDRESS, dst, 0, INET4_SOURCE_ADDRESS_LENGTH);
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
                byte[] dst = new byte[INET4_DESTINATION_ADDRESS_LENGTH];
                content().getBytes(INET4_DESTINATION_ADDRESS, dst, 0, INET4_DESTINATION_ADDRESS_LENGTH);
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
        final byte[] data = new byte[content().readableBytes() - INET4_HEADER_LENGTH];
        content().getBytes(INET4_HEADER_LENGTH, data);
        return data;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
                .append('[')
                .append("id=").append(identification())
                .append(", len=").append(totalLength())
                .append(", src=").append(sourceAddress().getHostAddress())
                .append(", dst=").append(destinationAddress().getHostAddress())
                .append(']').toString();
    }

    public boolean verifyChecksum() {
        return calculateChecksum(content()) == 0;
    }

    public static int calculateChecksum(final ByteBuf buf) {
        int sum = 0;
        for (int i = 0; i < INET4_HEADER_LENGTH; i += 2) {
            sum += buf.getUnsignedShort(i);
        }
        return (~((sum & 0xffff) + (sum >> 16))) & 0xffff;
    }

    @SuppressWarnings({ "java:S107", "UnusedReturnValue" })
    public static ByteBuf populatePacket(final ByteBuf buf,
                                         final int typeOfService,
                                         final int identification,
                                         final int flags,
                                         final int fragmentOffset,
                                         final int timeToLive,
                                         final int protocol,
                                         final boolean calculateChecksum,
                                         final Inet4Address sourceAddress,
                                         final Inet4Address destinationAddress,
                                         final byte[] data) {
        // version & ihl
        final int version = 4;
        final int ihl = 5;

        int versionIhl = 0;
        versionIhl |= (version & 0xf) << 4;
        versionIhl |= ihl & 0xf;
        buf.setByte(INET4_VERSION_AND_INTERNET_HEADER_LENGTH, versionIhl);

        // type of service
        buf.setShort(INET4_TYPE_OF_SERVICE, typeOfService);

        // total length
        final int totalLength = ihl * 4 + data.length;
        buf.setShort(INET4_TOTAL_LENGTH, totalLength);

        // identification
        buf.setShort(INET4_IDENTIFICATION, identification);

        // flags & fragment offset
        int flagsFragmentOffset = 0;
        flagsFragmentOffset |= (flags & 0x7) << 5;
        flagsFragmentOffset |= (fragmentOffset & 0x1f) << 3;
        buf.setByte(INET4_FLAGS_AND_FRAGMENT_OFFSET, flagsFragmentOffset);

        // time to live
        buf.setByte(INET4_TIME_TO_LIVE, timeToLive);

        // protocol
        buf.setByte(INET4_PROTOCOL, protocol);

        // source address
        buf.setBytes(INET4_SOURCE_ADDRESS, sourceAddress.getAddress());

        // destination address
        buf.setBytes(INET4_DESTINATION_ADDRESS, destinationAddress.getAddress());

        // header checksum
        if (calculateChecksum) {
            final int headerChecksum = calculateChecksum(buf);
            buf.setShort(INET4_HEADER_CHECKSUM, headerChecksum);
        }

        // data
        buf.setBytes(INET4_HEADER_LENGTH, data);

        buf.setIndex(0, totalLength);

        return buf;
    }

    @SuppressWarnings({ "java:S107", "UnusedReturnValue" })
    public static ByteBuf populatePacket(final ByteBuf buf,
                                         final int typeOfService,
                                         final int identification,
                                         final int flags,
                                         final int fragmentOffset,
                                         final int timeToLive,
                                         final InetProtocol protocol,
                                         final boolean calculateChecksum,
                                         final Inet4Address sourceAddress,
                                         final Inet4Address destinationAddress,
                                         final byte[] data) {
        return populatePacket(buf, typeOfService, identification, flags, fragmentOffset, timeToLive, protocol.decimal, calculateChecksum, sourceAddress, destinationAddress, data);
    }
}

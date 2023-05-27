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

import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import org.drasyl.channel.tun.jna.TunDevice;
import org.drasyl.channel.tun.jna.darwin.DarwinTunDevice;
import org.drasyl.channel.tun.jna.linux.LinuxTunDevice;
import org.drasyl.channel.tun.jna.windows.WindowsTunDevice;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link io.netty.channel.Channel} implementation that can be used to send or receive packets
 * over a TUN interface.
 * <p>
 * To initialize a {@link TunChannel}, create a {@link TunAddress} object containing the desired
 * name of the TUN device to open, and then bind the {@link TunChannel} to the address.
 * <p>
 * To send packets into the device (causing them to be received by the local host's network stack)
 * create a {@link TunPacket} object and call {@link io.netty.channel.Channel#write(Object)}.
 * <p>
 * When the host's network stack sends packets out via the device, the packets are delivered to the
 * channel causing a {@link io.netty.channel.ChannelInboundHandler#channelRead(ChannelHandlerContext,
 * Object)} invocation.
 */
public class TunChannel extends AbstractChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(TunPacket.class) + ')';
    final Runnable readTask = this::doRead;
    private final TunChannelConfig config = new DefaultTunChannelConfig(this);
    private final List<Object> readBuf = new ArrayList<>();
    private boolean readPending;
    private final EventLoop readLoop = new DefaultEventLoop();
    private TunDevice device;

    public TunChannel() {
        super(null);
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public TunChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return device == null || !device.isClosed();
    }

    @Override
    public boolean isActive() {
        return device != null && isOpen();
    }

    @Override
    protected SocketAddress localAddress0() {
        if (device != null) {
            return device.localAddress();
        }
        else {
            return null;
        }
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doBind(final SocketAddress localAddress) throws Exception {
        if (PlatformDependent.isOsx()) {
            device = DarwinTunDevice.open(((TunAddress) localAddress).ifName(), config.getMtu());
        }
        else if (PlatformDependent.isWindows()) {
            device = WindowsTunDevice.open(((TunAddress) localAddress).ifName());
        }
        else {
            device = LinuxTunDevice.open(((TunAddress) localAddress).ifName(), config.getMtu());
        }
    }

    @Override
    protected void doDisconnect() throws Exception {
        // do nothing
    }

    @Override
    protected void doClose() throws Exception {
        if (device != null) {
            device.close();
        }
    }

    /**
     * Read messages into the given array and return the amount which was read.
     */
    @SuppressWarnings("java:S112")
    protected int doReadMessages(List<Object> msgs) throws Exception {
        final TunPacket msg = device.readPacket(alloc());
        msgs.add(msg);
        return 1;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        while (true) {
            final Object msg = in.current();
            if (msg == null) {
                break;
            }

            try {
                device.writePacket(alloc(), (TunPacket) (((TunPacket) msg).retain()));
            }
            finally {
                in.remove();
            }
        }
    }

    @Override
    protected Object filterOutboundMessage(Object msg) {
        if (msg instanceof TunPacket) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    @SuppressWarnings({ "java:S135", "java:S1117", "java:S1181", "java:S1874", "java:S3776" })
    private void doRead() {
        if (!readPending) {
            return;
        }
        readPending = false;

        final ChannelConfig config = config();
        final ChannelPipeline pipeline = pipeline();
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
        allocHandle.reset(config);

        // read messages until RecvByteBuf is full
        boolean closed = false;
        Throwable exception = null;
        try {
            do {
                int localRead = doReadMessages(readBuf);
                if (localRead == 0) {
                    break;
                }
                if (localRead < 0) {
                    closed = true;
                    break;
                }

                allocHandle.incMessagesRead(localRead);
            } while (allocHandle.continueReading());
        }
        catch (final Throwable t) {
            exception = t;
        }

        // process read messages
        boolean readData = false;
        int size = readBuf.size();
        if (size > 0) {
            readData = true;
            for (int i = 0; i < size; i++) {
                readPending = false;
                pipeline.fireChannelRead(readBuf.get(i));
            }
            readBuf.clear();
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
        }

        if (exception != null) {
            if (exception instanceof IOException) {
                closed = true;
            }

            if (isOpen()) {
                pipeline.fireExceptionCaught(exception);
            }
        }

        if (closed) {
            if (isOpen()) {
                unsafe().close(unsafe().voidPromise());
            }
        }
        else if (readPending || config.isAutoRead() || !readData && isActive()) {
            read();
        }
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new TunChannelUnsafe();
    }

    @Override
    protected boolean isCompatible(final EventLoop loop) {
        return loop instanceof DefaultEventLoop;
    }

    @Override
    protected void doBeginRead() {
        if (readPending) {
            return;
        }
        if (!isActive()) {
            return;
        }

        readPending = true;
        readLoop.execute(readTask);
    }

    public TunDevice device() {
        return device;
    }

    private class TunChannelUnsafe extends AbstractUnsafe {
        @Override
        public void connect(final SocketAddress remoteAddress,
                            final SocketAddress localAddress,
                            final ChannelPromise promise) {
            throw new AlreadyConnectedException();
        }
    }
}

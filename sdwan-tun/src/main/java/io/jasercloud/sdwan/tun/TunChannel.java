package io.jasercloud.sdwan.tun;

import io.jasercloud.sdwan.tun.linux.LinuxTunDevice;
import io.jasercloud.sdwan.tun.windows.WinTunDevice;
import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.NetworkInterfaceUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class TunChannel extends AbstractChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private TunChannelConfig channelConfig;
    private Runnable readTask = new Runnable() {
        @Override
        public void run() {
            doRead();
        }
    };
    private boolean readPending;
    private EventLoop readLoop = new DefaultEventLoop();
    private List<Object> readBuf = new ArrayList<>();

    private TunAddress tunAddress;
    private TunDevice tunDevice;

    public TunChannel() {
        super(null);
        channelConfig = new TunChannelConfig(this);
    }

    @Override
    protected SocketAddress localAddress0() {
        return tunAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return tunAddress;
    }

    public void setAddress(String ip, int maskBits) throws Exception {
        tunDevice.setIP(ip, maskBits);
        waitAddress(ip, 30 * 1000);
        tunAddress.setVip(ip);
    }

    public void addRoute(NetworkInterfaceInfo interfaceInfo, String route, String ip) throws Exception {
        tunDevice.addRoute(interfaceInfo, route, ip);
    }

    private void waitAddress(String vip, int timeout) throws Exception {
        long s = System.currentTimeMillis();
        while (true) {
            NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(vip);
            if (null != networkInterfaceInfo) {
                return;
            }
            long e = System.currentTimeMillis();
            long diff = e - s;
            if (diff > timeout) {
                throw new TimeoutException();
            }
            Thread.sleep(100);
        }
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        tunAddress = (TunAddress) localAddress;
        String ethName = tunAddress.getEthName();
        String tunName = tunAddress.getTunName();
        String type = "jaspercloud";
        String guid = UUID.randomUUID().toString();
        if (PlatformDependent.isOsx()) {
        } else if (PlatformDependent.isWindows()) {
            tunDevice = new WinTunDevice(tunName, type, guid);
        } else {
            tunDevice = new LinuxTunDevice(ethName, tunName, type, guid);
        }
        tunDevice.open();
        Integer mtu = config().getOption(TunChannelConfig.MTU);
        tunDevice.setMTU(mtu);
    }

    @Override
    protected void doClose() throws Exception {
        tunDevice.close();
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (readPending) {
            return;
        }
        if (!isActive()) {
            return;
        }
        readPending = true;
        readLoop.execute(readTask);
    }

    private void doRead() {
        if (!readPending) {
            // We have to check readPending here because the Runnable to read could have been scheduled and later
            // during the same read loop readPending was set to false.
            return;
        }
        // In OIO we should set readPending to false even if the read was not successful so we can schedule
        // another read on the event loop if no reads are done.
        readPending = false;

        final ChannelConfig config = config();
        final ChannelPipeline pipeline = pipeline();
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
        allocHandle.reset(config);

        boolean closed = false;
        Throwable exception = null;
        try {
            do {
                // Perform a read.
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
        } catch (Throwable t) {
            exception = t;
        }

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

            pipeline.fireExceptionCaught(exception);
        }

        if (closed) {
            if (isOpen()) {
                unsafe().close(unsafe().voidPromise());
            }
        } else if (readPending || config.isAutoRead() || !readData && isActive()) {
            // Reading 0 bytes could mean there is a SocketTimeout and no data was actually read, so we
            // should execute read() again because no data may have been read.
            read();
        }
    }

    private int doReadMessages(List<Object> readBuf) {
        ByteBuf byteBuf = tunDevice.readPacket(config().getAllocator());
        byteBuf.markReaderIndex();
        byte version = (byte) (byteBuf.readUnsignedByte() >> 4);
        if (4 != version) {
            //read ipv4 only
            byteBuf.release();
            return 0;
        }
        byteBuf.resetReaderIndex();
        readBuf.add(byteBuf);
        return 1;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        while (true) {
            final Object msg = in.current();
            if (msg == null) {
                break;
            }
            if (!(msg instanceof ByteBuf)) {
                break;
            }
            try {
                ByteBuf byteBuf = (ByteBuf) msg;
                tunDevice.writePacket(alloc(), byteBuf);
            } finally {
                in.remove();
            }
        }
    }

    @Override
    protected void doDisconnect() throws Exception {

    }

    @Override
    protected TunChannelUnsafe newUnsafe() {
        return new TunChannelUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof DefaultEventLoop;
    }

    @Override
    public boolean isOpen() {
        return tunDevice == null || !tunDevice.isClosed();
    }

    @Override
    public boolean isActive() {
        return tunDevice != null && isOpen();
    }

    @Override
    public ChannelConfig config() {
        return channelConfig;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    private class TunChannelUnsafe extends AbstractUnsafe {

        @Override
        public void connect(final SocketAddress remoteAddress,
                            final SocketAddress localAddress,
                            final ChannelPromise promise) {
        }
    }

    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap = new Bootstrap()
                .group(new DefaultEventLoopGroup())
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, 1500)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
                                System.out.println();
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress("tun", "eth0"));
        TunChannel channel = (TunChannel) future.syncUninterruptibly().channel();
        channel.setAddress("192.168.1.1", 24);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

}

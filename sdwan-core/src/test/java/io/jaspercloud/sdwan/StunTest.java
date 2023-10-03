package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.Data;
import org.slf4j.impl.StaticLoggerBinder;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StunTest {

    public static final byte[] Cookie = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xa4, (byte) 0x42};

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        root.setLevel(Level.INFO);
        System.setProperty("io.netty.noPreferDirect", "true");
        new StunTest().run();
    }

    private Map<String, CompletableFuture<Packet>> futureMap = new ConcurrentHashMap<>();

    private InetSocketAddress local = new InetSocketAddress("0.0.0.0", 888);
    private InetSocketAddress target = new InetSocketAddress("stun.miwifi.com", 3478);
    private Channel channel;

    private void run() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<Packet>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
                                Message message = packet.content();
                                CompletableFuture<Packet> future = futureMap.remove(message.getTranId());
                                if (null != future) {
                                    future.complete(packet);
                                }
                            }
                        });
                    }
                });
        channel = bootstrap.bind(local).sync().channel();
        String mapping = getMappingBehavior();
        String filtering = getFilteringBehavior();
        channel.closeFuture().sync();
    }

    private String getFilteringBehavior() throws Exception {
        String status;
        InetSocketAddress inetMappedAddress1;
        try {
            Message message = new Message(MessageType.BindRequest);
            Packet request = new Packet(message, target);
            CompletableFuture<Packet> future = new CompletableFuture<>();
            futureMap.put(request.content().getTranId(), future);
            channel.writeAndFlush(request);
            Packet response = future.get(1000, TimeUnit.MILLISECONDS);
            Map<AttrType, Attr> attrs = response.content().getAttrs();
            AddressAttr mappedAddress = (AddressAttr) attrs.get(AttrType.MappedAddress);
            inetMappedAddress1 = new InetSocketAddress(mappedAddress.getIp(), mappedAddress.getPort());
            if (Objects.equals(local, inetMappedAddress1)) {
                status = "internet";
                return status;
            }
        } catch (TimeoutException e) {
            status = "blocked";
            return status;
        }
        try {
            Message message = new Message(MessageType.BindRequest);
            ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(true, true);
            message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
            Packet request = new Packet(message, target);
            CompletableFuture<Packet> future = new CompletableFuture<>();
            futureMap.put(request.content().getTranId(), future);
            channel.writeAndFlush(request);
            Packet response = future.get(1000, TimeUnit.MILLISECONDS);
            status = "EndpointIndependent";
            return status;
        } catch (TimeoutException e) {
            try {
                Message message = new Message(MessageType.BindRequest);
                ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(false, true);
                message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
                Packet request = new Packet(message, target);
                CompletableFuture<Packet> future = new CompletableFuture<>();
                futureMap.put(request.content().getTranId(), future);
                channel.writeAndFlush(request);
                Packet response = future.get(1000, TimeUnit.MILLISECONDS);
                status = "AddressDependent";
                return status;
            } catch (TimeoutException ex) {
                status = "AddressAndPortDependent";
                return status;
            }
        }
    }

    private String getMappingBehavior() throws Exception {
        String status;
        InetSocketAddress inetMappedAddress1;
        InetSocketAddress inetOtherAddress;
        try {
            Message message = new Message(MessageType.BindRequest);
            Packet request = new Packet(message, target);
            CompletableFuture<Packet> future = new CompletableFuture<>();
            futureMap.put(request.content().getTranId(), future);
            channel.writeAndFlush(request);
            Packet response = future.get(1000, TimeUnit.MILLISECONDS);
            Map<AttrType, Attr> attrs = response.content().getAttrs();
            AddressAttr mappedAddress = (AddressAttr) attrs.get(AttrType.MappedAddress);
            inetMappedAddress1 = new InetSocketAddress(mappedAddress.getIp(), mappedAddress.getPort());
            if (Objects.equals(local, inetMappedAddress1)) {
                status = "internet";
                return status;
            }
            AddressAttr otherAddress = (AddressAttr) attrs.get(AttrType.OtherAddress);
            inetOtherAddress = new InetSocketAddress(otherAddress.getIp(), otherAddress.getPort());
        } catch (TimeoutException e) {
            status = "blocked";
            return status;
        }
        Message message = new Message(MessageType.BindRequest);
        ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(true, true);
        message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
        Packet request = new Packet(message, inetOtherAddress);
        CompletableFuture<Packet> future = new CompletableFuture<>();
        futureMap.put(request.content().getTranId(), future);
        channel.writeAndFlush(request);
        Packet response = future.get(1000, TimeUnit.MILLISECONDS);
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr mappedAddress = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress inetMappedAddress2 = new InetSocketAddress(mappedAddress.getIp(), mappedAddress.getPort());
        if (Objects.equals(inetMappedAddress1, inetMappedAddress2)) {
            status = "EndpointIndependent";
            return status;
        } else {
            AddressAttr otherAddress = (AddressAttr) attrs.get(AttrType.OtherAddress);
            inetOtherAddress = new InetSocketAddress(otherAddress.getIp(), otherAddress.getPort());
            message = new Message(MessageType.BindRequest);
            changeRequestAttr = new ChangeRequestAttr(true, true);
            message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
            request = new Packet(message, inetOtherAddress);
            future = new CompletableFuture<>();
            futureMap.put(request.content().getTranId(), future);
            channel.writeAndFlush(request);
            response = future.get(1000, TimeUnit.MILLISECONDS);
        }
        attrs = response.content().getAttrs();
        mappedAddress = (AddressAttr) attrs.get(AttrType.MappedAddress);
        InetSocketAddress inetMappedAddress3 = new InetSocketAddress(mappedAddress.getIp(), mappedAddress.getPort());
        if (Objects.equals(inetMappedAddress2, inetMappedAddress3)) {
            status = "AddressDependent";
        } else {
            status = "AddressAndPortDependent";
        }
        return status;
    }

    public static class Decoder extends MessageToMessageDecoder<DatagramPacket> {
        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
            ByteBuf byteBuf = msg.content();
            int type = byteBuf.readUnsignedShort();
            int len = byteBuf.readUnsignedShort();
            //cookie
            byteBuf.skipBytes(Cookie.length);
            byte[] tranIdBytes = new byte[12];
            byteBuf.readBytes(tranIdBytes);
            String tranId = new String(tranIdBytes);
            Message message = new Message(MessageType.valueOf(type), tranId);
            ByteBuf attrs = byteBuf.readSlice(len);
            while (attrs.readableBytes() > 0) {
                int t = attrs.readUnsignedShort();
                int l = attrs.readUnsignedShort();
                ByteBuf v = attrs.readSlice(l);
                int reserved = v.readUnsignedByte();
                int family = v.readUnsignedByte();
                int port = v.readUnsignedShort();
                byte[] bytes = new byte[4];
                v.readBytes(bytes);
                String ip = IPUtil.bytes2ip(bytes);
                Attr attr = new AddressAttr(ProtoFamily.valueOf(family), ip, port);
                message.getAttrs().put(AttrType.valueOf(t), attr);
            }
            Packet packet = new Packet(message, msg.sender());
            out.add(packet);
        }
    }

    public static class Encoder extends MessageToMessageEncoder<Packet> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Packet msg, List<Object> out) throws Exception {
            Channel channel = ctx.channel();
            Message message = msg.content();
            ByteBuf attrsByteBuf = channel.alloc().heapBuffer();
            for (Map.Entry<AttrType, Attr> entry : message.getAttrs().entrySet()) {
                AttrType key = entry.getKey();
                Attr value = entry.getValue();
                if (AttrType.ChangeRequest.equals(key)) {
                    ChangeRequestAttr changeRequestAttr = (ChangeRequestAttr) value;
                    ByteBuf attrByteBuf = channel.alloc().heapBuffer();
                    attrByteBuf.writeShort(key.code);
                    attrByteBuf.writeShort(4);
                    int flag = 0;
                    if (changeRequestAttr.getChangeIP()) {
                        flag |= 0b100;
                    }
                    if (changeRequestAttr.getChangePort()) {
                        flag |= 0b10;
                    }
                    attrByteBuf.writeInt(flag);
                    attrsByteBuf.writeBytes(attrByteBuf);
                }
            }
            ByteBuf byteBuf = channel.alloc().heapBuffer();
            byteBuf.writeShort(message.getMessageType().code);
            byteBuf.writeShort(attrsByteBuf.readableBytes());
            byteBuf.writeBytes(Cookie);
            byteBuf.writeBytes(message.getTranId().getBytes());
            byteBuf.writeBytes(attrsByteBuf);
            DatagramPacket datagramPacket = new DatagramPacket(byteBuf, msg.recipient());
            out.add(datagramPacket);
        }
    }

    @Data
    public static class Message {

        private MessageType messageType;
        private String tranId = UUID.randomUUID().toString()
                .replaceAll("\\-", "")
                .substring(0, 12);
        private Map<AttrType, Attr> attrs = new HashMap<>();

        public Message() {
        }

        public Message(MessageType messageType) {
            this.messageType = messageType;
        }

        public Message(MessageType messageType, String tranId) {
            this.messageType = messageType;
            this.tranId = tranId;
        }
    }

    @Data
    public static class Attr {

    }

    @Data
    public static class ChangeRequestAttr extends Attr {

        private Boolean changeIP;
        private Boolean changePort;

        public ChangeRequestAttr() {
        }

        public ChangeRequestAttr(Boolean changeIP, Boolean changePort) {
            this.changeIP = changeIP;
            this.changePort = changePort;
        }
    }

    @Data
    public static class AddressAttr extends Attr {

        private ProtoFamily family;
        private String ip;
        private Integer port;

        public AddressAttr() {
        }

        public AddressAttr(ProtoFamily family, String ip, Integer port) {
            this.family = family;
            this.ip = ip;
            this.port = port;
        }
    }

    public static class Packet extends DefaultAddressedEnvelope<Message, InetSocketAddress> {

        public Packet(Message message, InetSocketAddress recipient) {
            super(message, recipient);
        }
    }

    public enum MessageType {

        BindRequest(0x0001),
        BindResponse(0x0101);
        private int code;

        MessageType(int code) {
            this.code = code;
        }

        public static MessageType valueOf(int code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }

    public enum ProtoFamily {

        IPv4(1),
        IPv6(2);
        private int code;

        ProtoFamily(int code) {
            this.code = code;
        }

        public static ProtoFamily valueOf(int code) {
            for (ProtoFamily protoFamily : values()) {
                if (protoFamily.code == code) {
                    return protoFamily;
                }
            }
            return null;
        }
    }

    public enum AttrType {

        MappedAddress(0x0001),
        ChangeRequest(0x0003),
        ResponseOrigin(0x802b),
        OtherAddress(0x802c),
        XorMappedAddress(0x0020);

        private int code;

        AttrType(int code) {
            this.code = code;
        }

        public static AttrType valueOf(int code) {
            for (AttrType attrType : values()) {
                if (attrType.code == code) {
                    return attrType;
                }
            }
            return null;
        }
    }

}

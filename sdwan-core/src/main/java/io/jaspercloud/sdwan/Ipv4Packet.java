package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.InetAddress;

public class Ipv4Packet {

    private short version;
    private short headerLen;
    private short diffServices;
    private int totalLen;
    private int id;
    private int flags;
    private short liveTime;
    private short protocol;
    private int checksum;
    private InetAddress srcIP;
    private InetAddress dstIP;
    private ByteBuf payload;

    public int getVersion() {
        return version;
    }

    public int getHeaderLen() {
        return headerLen;
    }

    public short getDiffServices() {
        return diffServices;
    }

    public int getTotalLen() {
        return totalLen;
    }

    public int getId() {
        return id;
    }

    public int getFlags() {
        return flags;
    }

    public short getLiveTime() {
        return liveTime;
    }

    public short getProtocol() {
        return protocol;
    }

    public int getChecksum() {
        return checksum;
    }

    public InetAddress getSrcIP() {
        return srcIP;
    }

    public InetAddress getDstIP() {
        return dstIP;
    }

    public void setSrcIP(InetAddress srcIP) {
        this.srcIP = srcIP;
    }

    public void setDstIP(InetAddress dstIP) {
        this.dstIP = dstIP;
    }

    public ByteBuf getPayload() {
        payload.resetReaderIndex();
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }

    private Ipv4Packet() {

    }

    public static Ipv4Packet decode(ByteBuf byteBuf) throws Exception {
        Ipv4Packet ipv4Packet = new Ipv4Packet();
        short head = byteBuf.readUnsignedByte();
        ipv4Packet.version = (byte) (head >> 4);
        ipv4Packet.headerLen = (byte) ((head & 0b00001111) * 4);
        ipv4Packet.diffServices = byteBuf.readUnsignedByte();
        ipv4Packet.totalLen = byteBuf.readUnsignedShort();
        ipv4Packet.id = byteBuf.readUnsignedShort();
        ipv4Packet.flags = byteBuf.readUnsignedShort();
        ipv4Packet.liveTime = byteBuf.readUnsignedByte();
        ipv4Packet.protocol = byteBuf.readUnsignedByte();
        ipv4Packet.checksum = byteBuf.readUnsignedShort();
        byte[] tmp = new byte[4];
        byteBuf.readBytes(tmp);
        ipv4Packet.srcIP = InetAddress.getByAddress(tmp);
        byteBuf.readBytes(tmp);
        ipv4Packet.dstIP = InetAddress.getByAddress(tmp);
        ByteBuf buf = byteBuf.readBytes(byteBuf.readableBytes());
        buf.markReaderIndex();
        ipv4Packet.payload = buf;
        return ipv4Packet;
    }

    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte head = (byte) ((version << 4) | (headerLen / 4));
        byteBuf.writeByte(head);
        byteBuf.writeByte(diffServices);
        byteBuf.writeShort(totalLen);
        byteBuf.writeShort(id);
        byteBuf.writeShort(flags);
        byteBuf.writeByte(liveTime);
        byteBuf.writeByte(protocol);
        int calcChecksum = calcChecksum();
//        Assert.isTrue(calcChecksum == checksum);
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeBytes(srcIP.getAddress());
        byteBuf.writeBytes(dstIP.getAddress());
        byteBuf.writeBytes(getPayload());
        return byteBuf;
    }

    public byte[] encodeBytes() {
        ByteBuf byteBuf = encode();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public int calcChecksum() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte head = (byte) ((version << 4) | (headerLen / 4));
        byteBuf.writeByte(head);
        byteBuf.writeByte(diffServices);
        byteBuf.writeShort(totalLen);
        byteBuf.writeShort(id);
        byteBuf.writeShort(flags);
        byteBuf.writeByte(liveTime);
        byteBuf.writeByte(protocol);
        byteBuf.writeShort(0);
        byteBuf.writeBytes(srcIP.getAddress());
        byteBuf.writeBytes(dstIP.getAddress());
        int sum = 0;
        while (byteBuf.readableBytes() > 0) {
            sum += byteBuf.readUnsignedShort();
        }
        int h = sum >> 16;
        int l = sum & 0b11111111_11111111;
        sum = (h + l);
        sum = 0b11111111_11111111 & ~sum;
        return sum;
    }
}

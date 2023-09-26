package io.jaspercloud.sdwan.tun;

import io.netty.buffer.ByteBuf;
import org.springframework.util.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Ipv4Packet {

    private short version;
    //IPV4+(TCP/UDP)
    private short headerLen;
    private short diffServices;
    private int totalLen;
    private int id;
    private int flags;
    private short liveTime;
    private int protocol;
    private int checksum;
    private InetAddress srcIP;
    private InetAddress dstIP;
    private ByteBuf payload;

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public short getHeaderLen() {
        return headerLen;
    }

    public void setHeaderLen(short headerLen) {
        this.headerLen = headerLen;
    }

    public short getDiffServices() {
        return diffServices;
    }

    public void setDiffServices(short diffServices) {
        this.diffServices = diffServices;
    }

    public int getTotalLen() {
        return totalLen;
    }

    public void setTotalLen(int totalLen) {
        this.totalLen = totalLen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public short getLiveTime() {
        return liveTime;
    }

    public void setLiveTime(short liveTime) {
        this.liveTime = liveTime;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public InetAddress getSrcIP() {
        return srcIP;
    }

    public void setSrcIP(InetAddress srcIP) {
        this.srcIP = srcIP;
    }

    public InetAddress getDstIP() {
        return dstIP;
    }

    public void setDstIP(InetAddress dstIP) {
        this.dstIP = dstIP;
    }

    public ByteBuf getPayload() {
        payload.resetReaderIndex();
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        payload.markReaderIndex();
        this.payload = payload;
    }

    private Ipv4Packet() {

    }

    public static Ipv4Packet decode(ByteBuf byteBuf) {
        Ipv4Packet ipv4Packet = new Ipv4Packet();
        short head = byteBuf.readUnsignedByte();
        short version = (byte) (head >> 4);
        byte headLen = (byte) ((head & 0b00001111) * 4);
        short diffServices = byteBuf.readUnsignedByte();
        int totalLen = byteBuf.readUnsignedShort();
        int id = byteBuf.readUnsignedShort();
        int flags = byteBuf.readUnsignedShort();
        short liveTime = byteBuf.readUnsignedByte();
        int protocol = byteBuf.readUnsignedByte();
        int checksum = byteBuf.readUnsignedShort();
        byte[] srcIPBytes = new byte[4];
        byteBuf.readBytes(srcIPBytes);
        byte[] dstIPBytes = new byte[4];
        byteBuf.readBytes(dstIPBytes);
        ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());
        //set
        ipv4Packet.setVersion(version);
        ipv4Packet.setHeaderLen(headLen);
        ipv4Packet.setDiffServices(diffServices);
        ipv4Packet.setTotalLen(totalLen);
        ipv4Packet.setId(id);
        ipv4Packet.setFlags(flags);
        ipv4Packet.setLiveTime(liveTime);
        ipv4Packet.setProtocol(protocol);
        ipv4Packet.setChecksum(checksum);
        ipv4Packet.setSrcIP(getByAddress(srcIPBytes));
        ipv4Packet.setDstIP(getByAddress(dstIPBytes));
        ipv4Packet.setPayload(payload);
        return ipv4Packet;
    }

    public ByteBuf encode() {
        return encode(false);
    }

    public ByteBuf encode(boolean checksum) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byte head = (byte) ((version << 4) | (headerLen / 4));
        byteBuf.writeByte(head);
        byteBuf.writeByte(diffServices);
        byteBuf.writeShort(totalLen);
        byteBuf.writeShort(id);
        byteBuf.writeShort(flags);
        byteBuf.writeByte(liveTime);
        byteBuf.writeByte(protocol);
        int calcChecksum = calcChecksum();
        if (checksum) {
            Assert.isTrue(calcChecksum == getChecksum(), "checksum error");
        }
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeBytes(srcIP.getAddress());
        byteBuf.writeBytes(dstIP.getAddress());
        byteBuf.writeBytes(getPayload());
        return byteBuf;
    }

    private int calcChecksum() {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byte head = (byte) ((version << 4) | (headerLen / 4));
        byteBuf.writeByte(head);
        byteBuf.writeByte(diffServices);
        byteBuf.writeShort(totalLen);
        byteBuf.writeShort(id);
        byteBuf.writeShort(flags);
        byteBuf.writeByte(liveTime);
        byteBuf.writeByte(protocol);
        //checksum字段置为0
        byteBuf.writeShort(0);
        byteBuf.writeBytes(srcIP.getAddress());
        byteBuf.writeBytes(dstIP.getAddress());
        //数据长度为奇数，在该字节之后补一个字节
        if (0 != byteBuf.readableBytes() % 2) {
            byteBuf.writeByte(0);
        }
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

    private static InetAddress getByAddress(byte[] bytes) {
        try {
            InetAddress address = InetAddress.getByAddress(bytes);
            return address;
        } catch (UnknownHostException e) {
            return null;
        }
    }
}

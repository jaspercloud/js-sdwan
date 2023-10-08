package io.jasercloud.sdwan.tun;

import io.jaspercloud.sdwan.IPUtil;
import io.netty.buffer.ByteBuf;
import org.springframework.util.Assert;

public class Ipv4Packet implements IpPacket {

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
    private String srcIP;
    private String dstIP;
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

    public String getSrcIP() {
        return srcIP;
    }

    public void setSrcIP(String srcIP) {
        this.srcIP = srcIP;
    }

    public String getDstIP() {
        return dstIP;
    }

    public void setDstIP(String dstIP) {
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

    public static Ipv4Packet decodeMark(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        Ipv4Packet packet = decode(byteBuf);
        byteBuf.resetReaderIndex();
        return packet;
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
        ipv4Packet.setSrcIP(IPUtil.bytes2ip(srcIPBytes));
        ipv4Packet.setDstIP(IPUtil.bytes2ip(dstIPBytes));
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
        byteBuf.writeBytes(IPUtil.ip2bytes(srcIP));
        byteBuf.writeBytes(IPUtil.ip2bytes(dstIP));
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
        byteBuf.writeBytes(IPUtil.ip2bytes(srcIP));
        byteBuf.writeBytes(IPUtil.ip2bytes(dstIP));
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
}

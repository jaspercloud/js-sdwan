package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.IPUtil;
import io.netty.buffer.ByteBuf;
import org.springframework.util.Assert;

public class UdpPacket {

    private int srcPort;
    private int dstPort;
    private int len;
    private int checksum;
    private ByteBuf payload;

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public ByteBuf getPayload() {
        payload.resetReaderIndex();
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        payload.markReaderIndex();
        this.payload = payload;
    }

    public static UdpPacket decodeMark(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        UdpPacket packet = decode(byteBuf);
        byteBuf.resetReaderIndex();
        return packet;
    }

    public static UdpPacket decode(ByteBuf byteBuf) {
        int srcPort = byteBuf.readUnsignedShort();
        int dstPort = byteBuf.readUnsignedShort();
        int len = byteBuf.readUnsignedShort();
        int checksum = byteBuf.readUnsignedShort();
        ByteBuf payload = byteBuf.readSlice(len - 8);
        //set
        UdpPacket udpPacket = new UdpPacket();
        udpPacket.setSrcPort(srcPort);
        udpPacket.setDstPort(dstPort);
        udpPacket.setLen(len);
        udpPacket.setChecksum(checksum);
        udpPacket.setPayload(payload);
        return udpPacket;
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet) {
        return encode(ipv4Packet, false);
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet, boolean checksum) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byteBuf.writeShort(getSrcPort());
        byteBuf.writeShort(getDstPort());
        byteBuf.writeShort(getLen());
        int calcChecksum = calcChecksum(ipv4Packet);
        if (checksum) {
            Assert.isTrue(calcChecksum == getChecksum(), "checksum error");
        }
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeBytes(getPayload());
        return byteBuf;
    }

    private int calcChecksum(Ipv4Packet ipv4Packet) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        //ipHeader
        byteBuf.writeBytes(IPUtil.ip2bytes(ipv4Packet.getSrcIP()));
        byteBuf.writeBytes(IPUtil.ip2bytes(ipv4Packet.getDstIP()));
        byteBuf.writeByte(0);
        byteBuf.writeByte(ipv4Packet.getProtocol());
        byteBuf.writeShort(ipv4Packet.getPayload().readableBytes());
        //udp
        byteBuf.writeShort(getSrcPort());
        byteBuf.writeShort(getDstPort());
        byteBuf.writeShort(getLen());
        byteBuf.writeShort(0);
        byteBuf.writeBytes(getPayload());
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

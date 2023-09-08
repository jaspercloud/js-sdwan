package io.jaspercloud.sdwan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TcpPacket {

    private int srcPort;
    private int dstPort;
    private long seq;
    private long ack;
    private int flags;
    private int headLen;
    private int reservedFlag;
    private int accurateECNFlag;
    private int echoFlag;
    private int urgFlag;
    private int ackFlag;
    private int pshFlag;
    private int rstFlag;
    private int synFlag;
    private int finFlag;
    private int window;
    private int checksum;
    private int urgentPointer;
    private ByteBuf optionsByteBuf;

    public int getSrcPort() {
        return srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public long getSeq() {
        return seq;
    }

    public long getAck() {
        return ack;
    }

    public int getFlags() {
        return flags;
    }

    public int getHeadLen() {
        return headLen;
    }

    public int getReservedFlag() {
        return reservedFlag;
    }

    public int getAccurateECNFlag() {
        return accurateECNFlag;
    }

    public int getEchoFlag() {
        return echoFlag;
    }

    public int getUrgFlag() {
        return urgFlag;
    }

    public int getAckFlag() {
        return ackFlag;
    }

    public int getPshFlag() {
        return pshFlag;
    }

    public int getRstFlag() {
        return rstFlag;
    }

    public int getSynFlag() {
        return synFlag;
    }

    public int getFinFlag() {
        return finFlag;
    }

    public int getWindow() {
        return window;
    }

    public int getChecksum() {
        return checksum;
    }

    public int getUrgentPointer() {
        return urgentPointer;
    }

    public ByteBuf getOptionsByteBuf() {
        optionsByteBuf.resetReaderIndex();
        return optionsByteBuf;
    }

    private TcpPacket() {

    }

    public static TcpPacket decode(ByteBuf byteBuf) throws Exception {
        TcpPacket tcpPacket = new TcpPacket();
        tcpPacket.srcPort = byteBuf.readUnsignedShort();
        tcpPacket.dstPort = byteBuf.readUnsignedShort();
        tcpPacket.seq = byteBuf.readUnsignedInt();
        tcpPacket.ack = byteBuf.readUnsignedInt();
        int flags = byteBuf.readUnsignedShort();
        tcpPacket.flags = flags;
        tcpPacket.headLen = ((flags & 0b11110000_00000000) >> 12) * 4;
        tcpPacket.reservedFlag = (flags & 0b00000001_00000000) >> 8;
        tcpPacket.accurateECNFlag = (flags & 0b00000000_10000000) >> 7;
        tcpPacket.echoFlag = (flags & 0b00000000_01000000) >> 6;
        tcpPacket.urgFlag = (flags & 0b00000000_00100000) >> 5;
        tcpPacket.ackFlag = (flags & 0b00000000_00010000) >> 4;
        tcpPacket.pshFlag = (flags & 0b00000000_00001000) >> 3;
        tcpPacket.rstFlag = (flags & 0b00000000_00000100) >> 2;
        tcpPacket.synFlag = (flags & 0b00000000_00000010) >> 1;
        tcpPacket.finFlag = (flags & 0b00000000_00000001) >> 0;
        tcpPacket.window = byteBuf.readUnsignedShort();
        tcpPacket.checksum = byteBuf.readUnsignedShort();
        tcpPacket.urgentPointer = byteBuf.readUnsignedShort();
        ByteBuf buf = byteBuf.readBytes(byteBuf.readableBytes());
        buf.markReaderIndex();
        tcpPacket.optionsByteBuf = buf;
        return tcpPacket;
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShort(srcPort);
        byteBuf.writeShort(dstPort);
        byteBuf.writeInt((int) seq);
        byteBuf.writeInt((int) ack);
        byteBuf.writeShort(flags);
        byteBuf.writeShort(window);
        int calcChecksum = calcChecksum(ipv4Packet);
//        Assert.isTrue(calcChecksum == checksum);
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeShort(urgentPointer);
        byteBuf.writeBytes(getOptionsByteBuf());
        return byteBuf;
    }

    public int calcChecksum(Ipv4Packet ipv4Packet) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes(ipv4Packet.getSrcIP().getAddress());
        byteBuf.writeBytes(ipv4Packet.getDstIP().getAddress());
        byteBuf.writeByte(0);
        byteBuf.writeByte(ipv4Packet.getProtocol());
        byteBuf.writeShort(ipv4Packet.getPayload().readableBytes());
        byteBuf.writeShort(srcPort);
        byteBuf.writeShort(dstPort);
        byteBuf.writeInt((int) seq);
        byteBuf.writeInt((int) ack);
        byteBuf.writeShort(flags);
        byteBuf.writeShort(window);
        byteBuf.writeShort(0);
        byteBuf.writeShort(urgentPointer);
        byteBuf.writeBytes(getOptionsByteBuf());
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

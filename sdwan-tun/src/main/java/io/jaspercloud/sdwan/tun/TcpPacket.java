package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.IPUtil;
import io.netty.buffer.ByteBuf;
import org.springframework.util.Assert;

public class TcpPacket {

    private int srcPort;
    private int dstPort;
    private long seq;
    private long ack;
    private int flags;
    //TCPHead+Options
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

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public long getAck() {
        return ack;
    }

    public void setAck(long ack) {
        this.ack = ack;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getHeadLen() {
        return headLen;
    }

    public void setHeadLen(int headLen) {
        this.headLen = headLen;
    }

    public int getReservedFlag() {
        return reservedFlag;
    }

    public void setReservedFlag(int reservedFlag) {
        this.reservedFlag = reservedFlag;
    }

    public int getAccurateECNFlag() {
        return accurateECNFlag;
    }

    public void setAccurateECNFlag(int accurateECNFlag) {
        this.accurateECNFlag = accurateECNFlag;
    }

    public int getEchoFlag() {
        return echoFlag;
    }

    public void setEchoFlag(int echoFlag) {
        this.echoFlag = echoFlag;
    }

    public int getUrgFlag() {
        return urgFlag;
    }

    public void setUrgFlag(int urgFlag) {
        this.urgFlag = urgFlag;
    }

    public int getAckFlag() {
        return ackFlag;
    }

    public void setAckFlag(int ackFlag) {
        this.ackFlag = ackFlag;
    }

    public int getPshFlag() {
        return pshFlag;
    }

    public void setPshFlag(int pshFlag) {
        this.pshFlag = pshFlag;
    }

    public int getRstFlag() {
        return rstFlag;
    }

    public void setRstFlag(int rstFlag) {
        this.rstFlag = rstFlag;
    }

    public int getSynFlag() {
        return synFlag;
    }

    public void setSynFlag(int synFlag) {
        this.synFlag = synFlag;
    }

    public int getFinFlag() {
        return finFlag;
    }

    public void setFinFlag(int finFlag) {
        this.finFlag = finFlag;
    }

    public int getWindow() {
        return window;
    }

    public void setWindow(int window) {
        this.window = window;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public int getUrgentPointer() {
        return urgentPointer;
    }

    public void setUrgentPointer(int urgentPointer) {
        this.urgentPointer = urgentPointer;
    }

    public ByteBuf getOptionsByteBuf() {
        optionsByteBuf.resetReaderIndex();
        return optionsByteBuf;
    }

    public void setOptionsByteBuf(ByteBuf optionsByteBuf) {
        optionsByteBuf.markReaderIndex();
        this.optionsByteBuf = optionsByteBuf;
    }

    public ByteBuf getPayload() {
        payload.resetReaderIndex();
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        payload.markReaderIndex();
        this.payload = payload;
    }

    private TcpPacket() {

    }

    public static TcpPacket decodeMark(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        TcpPacket packet = decode(byteBuf);
        byteBuf.resetReaderIndex();
        return packet;
    }

    public static TcpPacket decode(ByteBuf byteBuf) {
        TcpPacket tcpPacket = new TcpPacket();
        int srcPort = byteBuf.readUnsignedShort();
        int dstPort = byteBuf.readUnsignedShort();
        long seq = byteBuf.readUnsignedInt();
        long ack = byteBuf.readUnsignedInt();
        int flags = byteBuf.readUnsignedShort();
        int tcpHeadLen = ((flags & 0b11110000_00000000) >> 12) * 4;
        int reservedFlag = (flags & 0b00000001_00000000) >> 8;
        int accurateECNFlag = (flags & 0b00000000_10000000) >> 7;
        int echoFlag = (flags & 0b00000000_01000000) >> 6;
        int urgFlag = (flags & 0b00000000_00100000) >> 5;
        int ackFlag = (flags & 0b00000000_00010000) >> 4;
        int pshFlag = (flags & 0b00000000_00001000) >> 3;
        int rstFlag = (flags & 0b00000000_00000100) >> 2;
        int synFlag = (flags & 0b00000000_00000010) >> 1;
        int finFlag = (flags & 0b00000000_00000001) >> 0;
        int window = byteBuf.readUnsignedShort();
        int checksum = byteBuf.readUnsignedShort();
        int urgentPointer = byteBuf.readUnsignedShort();
        ByteBuf optionsByteBuf = byteBuf.readSlice(tcpHeadLen - 20);
        ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());
        //set
        tcpPacket.setSrcPort(srcPort);
        tcpPacket.setDstPort(dstPort);
        tcpPacket.setSeq(seq);
        tcpPacket.setAck(ack);
        tcpPacket.setFlags(flags);
        tcpPacket.setHeadLen(tcpHeadLen);
        tcpPacket.setReservedFlag(reservedFlag);
        tcpPacket.setAccurateECNFlag(accurateECNFlag);
        tcpPacket.setEchoFlag(echoFlag);
        tcpPacket.setUrgFlag(urgFlag);
        tcpPacket.setAckFlag(ackFlag);
        tcpPacket.setPshFlag(pshFlag);
        tcpPacket.setRstFlag(rstFlag);
        tcpPacket.setSynFlag(synFlag);
        tcpPacket.setFinFlag(finFlag);
        tcpPacket.setWindow(window);
        tcpPacket.setChecksum(checksum);
        tcpPacket.setUrgentPointer(urgentPointer);
        tcpPacket.setOptionsByteBuf(optionsByteBuf);
        tcpPacket.setPayload(payload);
        return tcpPacket;
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet) {
        return encode(ipv4Packet, false);
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet, boolean checksum) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byteBuf.writeShort(srcPort);
        byteBuf.writeShort(dstPort);
        byteBuf.writeInt((int) seq);
        byteBuf.writeInt((int) ack);
        byteBuf.writeShort(flags);
        byteBuf.writeShort(window);
        int calcChecksum = calcChecksum(ipv4Packet);
        if (checksum) {
            Assert.isTrue(calcChecksum == getChecksum(), "checksum error");
        }
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeShort(urgentPointer);
        byteBuf.writeBytes(getOptionsByteBuf());
        byteBuf.writeBytes(getPayload());
        return byteBuf;
    }

    private int calcChecksum(Ipv4Packet ipv4Packet) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        try {
            //ipHeader
            byteBuf.writeBytes(IPUtil.ip2bytes(ipv4Packet.getSrcIP()));
            byteBuf.writeBytes(IPUtil.ip2bytes(ipv4Packet.getDstIP()));
            byteBuf.writeByte(0);
            byteBuf.writeByte(ipv4Packet.getProtocol());
            byteBuf.writeShort(ipv4Packet.getPayload().readableBytes());
            //tcp
            byteBuf.writeShort(srcPort);
            byteBuf.writeShort(dstPort);
            byteBuf.writeInt((int) seq);
            byteBuf.writeInt((int) ack);
            byteBuf.writeShort(flags);
            byteBuf.writeShort(window);
            //checksum字段置为0
            byteBuf.writeShort(0);
            byteBuf.writeShort(urgentPointer);
            byteBuf.writeBytes(getOptionsByteBuf());
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
        } finally {
            byteBuf.release();
        }
    }
}

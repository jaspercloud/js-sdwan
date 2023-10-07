//package io.jaspercloud.sdwan;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import org.junit.jupiter.api.Test;
//import org.savarese.vserv.tcpip.TCPPacket;
//
//import java.io.ByteArrayOutputStream;
//
//public class RawSocketTest {
//
//    @Test
//    public void test() throws Exception {
//        String data = "4500003e1fa3400080060000c0de0042c0de0899";
//        data += "f4a10cea59ff9022258dce195018c01f8ac80000";
//        data += "1200000003534554204e414d455320757466386d6234";
//        byte[] bytes = toBytes(data);
//        Ipv4Packet ipv4Packet = Ipv4Packet.decode(Unpooled.wrappedBuffer(bytes));
//        TcpPacket tcpPacket = TcpPacket.decode(ipv4Packet.getPayload());
//        ByteBuf byteBuf = Unpooled.buffer();
//        byteBuf.writeBytes(ipv4Packet.getSrcIP().getAddress());
//        byteBuf.writeBytes(ipv4Packet.getDstIP().getAddress());
//        byteBuf.writeByte(0);
//        byteBuf.writeByte(0x06);
//        byteBuf.writeShort(ipv4Packet.getPayload().readableBytes());
//        byteBuf.writeShort(tcpPacket.getSrcPort());
//        byteBuf.writeShort(tcpPacket.getDstPort());
//        byteBuf.writeInt((int) tcpPacket.getSeq());
//        byteBuf.writeInt((int) tcpPacket.getAck());
//        byteBuf.writeShort(tcpPacket.getFlags());
//        byteBuf.writeShort(tcpPacket.getWindow());
//        byteBuf.writeShort(0);
//        byteBuf.writeShort(tcpPacket.getUrgentPointer());
//        byteBuf.writeBytes(tcpPacket.getOptionsByteBuf());
//        if (0 != byteBuf.readableBytes() % 2) {
//            byteBuf.writeByte(0);
//        }
//        int sum = 0;
//        while (byteBuf.readableBytes() > 0) {
//            sum += byteBuf.readUnsignedShort();
//        }
//        int h = sum >> 16;
//        int l = sum & 0b11111111_11111111;
//        sum = (h + l);
//        sum = 0b11111111_11111111 & ~sum;
//
//        TCPPacket packet = new TCPPacket(bytes.length);
//        packet.setData(bytes);
//        int ipChecksum1 = packet.getIPChecksum();
//        packet.computeIPChecksum(true);
//        int ipChecksum2 = packet.getIPChecksum();
//        int tcpChecksum1 = packet.getTCPChecksum();
//        packet.computeTCPChecksum(true);
//        int tcpChecksum2 = packet.getTCPChecksum();
//        System.out.println();
//    }
//
//    private byte[] toBytes(String data) {
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        char[] chars = data.toCharArray();
//        for (int i = 0; i < chars.length; i += 2) {
//            String s = String.valueOf(chars[i]) + String.valueOf(chars[i + 1]);
//            byte b = (byte) Integer.parseInt(s, 16);
//            stream.write(b);
//        }
//        byte[] bytes = stream.toByteArray();
//        return bytes;
//    }
//}

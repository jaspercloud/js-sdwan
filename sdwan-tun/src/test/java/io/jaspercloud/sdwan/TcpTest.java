package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.TcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;

public class TcpTest {

    public static void main(String[] args) throws Exception {
        {
            //SYN
            String data = "4510003c05fe40004006b26cc0de0043c0de0042";
            data += "e4863ffbd877a6b600000000a00272100c9b0000020405b40402080a001aa3480000000001030307";
            Ipv4Packet ipv4Packet = Ipv4Packet.decode(Unpooled.wrappedBuffer(toBytes(data)));
            TcpPacket tcpPacket = TcpPacket.decode(ipv4Packet.getPayload());
            ipv4Packet.setPayload(tcpPacket.encode(ipv4Packet, true));
            ByteBuf encode = ipv4Packet.encode(true);
            System.out.println();
        }
        {
            //PUSH
            String data = "4510002e060040004006b278c0de0043c0de0042";
            data += "e4863ffbd877a6b7854ba1c1501800e56cfd0000";
            data += "746573740d0a";
            Ipv4Packet ipv4Packet = Ipv4Packet.decode(Unpooled.wrappedBuffer(toBytes(data)));
            TcpPacket tcpPacket = TcpPacket.decode(ipv4Packet.getPayload());
            ipv4Packet.setPayload(tcpPacket.encode(ipv4Packet, true));
            ByteBuf encode = ipv4Packet.encode(true);
            System.out.println();
        }
        System.out.println();
    }

    private static byte[] toBytes(String data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        char[] chars = data.toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            String s = String.valueOf(chars[i]) + String.valueOf(chars[i + 1]);
            byte b = (byte) Integer.parseInt(s, 16);
            stream.write(b);
        }
        byte[] bytes = stream.toByteArray();
        return bytes;
    }
}

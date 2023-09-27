package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.UdpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;

public class UdpTest {

    public static void main(String[] args) {
        String data = "450000217d18400040113b72c0de0043c0de0042";
        data += "c96922b8000d9f96";
        data += "746573740a";
        ByteBuf byteBuf = Unpooled.wrappedBuffer(toBytes(data));
        Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
        UdpPacket udpPacket = UdpPacket.decode(ipv4Packet.getPayload());
        ipv4Packet.setPayload(udpPacket.encode(ipv4Packet, true));
        ByteBuf encode = ipv4Packet.encode(true);
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

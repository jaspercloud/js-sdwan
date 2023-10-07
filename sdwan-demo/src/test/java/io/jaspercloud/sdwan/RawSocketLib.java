//package io.jaspercloud.sdwan;
//
//import com.sun.jna.*;
//import com.sun.jna.ptr.IntByReference;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import org.drasyl.channel.tun.InetProtocol;
//
//import java.io.ByteArrayOutputStream;
//import java.util.Arrays;
//import java.util.Objects;
//
//import static io.jaspercloud.sdwan.RawSocket.*;
//
//public class RawSocketLib {
//
//    public static void main(String[] args) throws Exception {
//        String data = "45100038247a400040068b98c0de089fc0de0042";
//        data += "c8d704385c67227d00000000900272100bff0000020405b40402080a0166060700000000";
//        byte[] bytes = toBytes(data);
//        Ipv4Packet ipv4Packet = Ipv4Packet.decode(Unpooled.wrappedBuffer(bytes));
//
//        RawSocket rawSocket = new RawSocket(AF_INET, SOCK_RAW, IPPROTO_RAW);
//        IntByReference hdrincl = new IntByReference(1);
//        Memory memory = (Memory) hdrincl.getPointer();
//        int setsockopt = rawSocket.setsockopt(IPPROTO_IP, IP_HDRINCL, memory, (int) memory.size());
//        int bind = rawSocket.bind("0.0.0.0", 5566);
////        int writeSize = rawSocket.write(ipv4Packet.getDstIP().getHostAddress(), bytes);
//        System.out.println();
//
//        while (true) {
//            byte[] tmp = new byte[1024];
//            int recvSize = rawSocket.recvfrom(tmp);
//            if (-1 == recvSize) {
//                continue;
//            }
//            byte[] result = Arrays.copyOf(tmp, recvSize);
//            Ipv4Packet decode = Ipv4Packet.decode(Unpooled.wrappedBuffer(result));
////            if (!Objects.equals(decode.getSrcIP().getHostAddress(), ipv4Packet.getDstIP().getHostAddress())) {
////                continue;
////            }
//            if (!Objects.equals(decode.getProtocol(), 17)) {
//                continue;
//            }
//            System.out.println(String.format("id=%s, proto=%s, src=%s, dst=%s",
//                    decode.getId(),
//                    InetProtocol.protocolByDecimal(decode.getProtocol()).toString(),
//                    decode.getSrcIP().getHostAddress(),
//                    decode.getDstIP().getHostAddress()));
//        }
////                int close = rawSocket.close();
//    }
//
//    private static byte[] toBytes(ByteBuf byteBuf) {
//        byte[] bytes = new byte[byteBuf.readableBytes()];
//        byteBuf.readBytes(bytes);
//        return bytes;
//    }
//
//    private static byte[] toBytes(String data) {
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

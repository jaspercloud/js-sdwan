//package io.jaspercloud.sdwan;
//
//import io.netty.channel.ChannelHandlerContext;
//import org.drasyl.channel.tun.Tun4Packet;
//
//import java.net.InetSocketAddress;
//import java.net.Socket;
//
//public class TwoWinTunTest {
//
//    public static void main(String[] args) throws Exception {
//        WinTun winTun1 = new WinTun("tun1", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                System.out.println("process1");
//            }
//        });
//        winTun1.start("10.1.0.2", 24);
//        WinTun winTun2 = new WinTun("tun2", new WinTun.TunnelDataHandler() {
//            @Override
//            public void process(ChannelHandlerContext ctx, Tun4Packet packet) throws Exception {
//                System.out.println("process2");
//            }
//        });
//        winTun2.start("10.1.0.3", 24);
//        Socket socket = new Socket();
//        socket.connect(new InetSocketAddress("10.1.0.4", 8888));
//    }
//}

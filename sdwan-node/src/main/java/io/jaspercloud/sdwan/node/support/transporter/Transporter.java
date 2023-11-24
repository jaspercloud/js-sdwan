//package io.jaspercloud.sdwan.node.support.transporter;
//
//import io.jaspercloud.sdwan.tun.TunChannel;
//import io.netty.buffer.ByteBuf;
//
//import java.net.InetSocketAddress;
//
//public interface Transporter {
//
//    void bind(TunChannel tunChannel);
//
//    interface Filter {
//
//        byte[] encode(InetSocketAddress address, byte[] bytes);
//
//        byte[] decode(InetSocketAddress address, byte[] bytes);
//    }
//}

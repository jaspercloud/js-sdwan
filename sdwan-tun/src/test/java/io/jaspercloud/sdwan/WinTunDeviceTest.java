package io.jaspercloud.sdwan;

import io.jasercloud.sdwan.tun.Ipv4Packet;
import io.jasercloud.sdwan.tun.windows.WinTunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.UUID;

public class WinTunDeviceTest {

    public static void main(String[] args) throws Exception {
        WinTunDevice tunDevice = new WinTunDevice("tun", "sdwan", UUID.randomUUID().toString());
        tunDevice.open();
        tunDevice.setIP("192.168.1.1", 24);
        tunDevice.setMTU(1500);
        UnpooledByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        while (true) {
            ByteBuf byteBuf = tunDevice.readPacket(allocator);
            byteBuf.markReaderIndex();
            byte version = (byte) (byteBuf.readUnsignedByte() >> 4);
            byteBuf.resetReaderIndex();
            if (4 == version) {
                Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
                System.out.println(String.format("%s -> %s", ipv4Packet.getSrcIP(), ipv4Packet.getDstIP()));
                byteBuf.resetReaderIndex();
                tunDevice.writePacket(allocator, byteBuf);
            }
        }
    }
}

package io.jasercloud.sdwan;

import io.jaspercloud.sdwan.IPUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.Map;

public class StunEncoder extends MessageToMessageEncoder<StunPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, StunPacket msg, List<Object> out) throws Exception {
        Channel channel = ctx.channel();
        StunMessage message = msg.content();
        ByteBuf attrsByteBuf = channel.alloc().heapBuffer();
        for (Map.Entry<AttrType, Attr> entry : message.getAttrs().entrySet()) {
            AttrType key = entry.getKey();
            if (AttrType.ChangeRequest.equals(key)) {
                processChangeRequest(channel, entry, attrsByteBuf);
            } else if (AttrType.MappedAddress.equals(key)) {
                processMappedAddress(channel, entry, attrsByteBuf);
            }
        }
        ByteBuf byteBuf = channel.alloc().heapBuffer();
        byteBuf.writeShort(message.getMessageType().getCode());
        byteBuf.writeShort(attrsByteBuf.readableBytes());
        byteBuf.writeBytes(StunMessage.Cookie);
        byteBuf.writeBytes(message.getTranId().getBytes());
        byteBuf.writeBytes(attrsByteBuf);
        DatagramPacket datagramPacket = new DatagramPacket(byteBuf, msg.recipient());
        out.add(datagramPacket);
    }

    private void processMappedAddress(Channel channel, Map.Entry<AttrType, Attr> entry, ByteBuf attrsByteBuf) {
        AttrType key = entry.getKey();
        AddressAttr addressAttr = (AddressAttr) entry.getValue();
        ByteBuf attrByteBuf = channel.alloc().heapBuffer();
        attrByteBuf.writeShort(key.getCode());
        attrByteBuf.writeShort(8);
        attrByteBuf.writeByte(0);
        attrByteBuf.writeByte(addressAttr.getFamily().getCode());
        attrByteBuf.writeShort(addressAttr.getPort());
        attrByteBuf.writeBytes(IPUtil.ip2bytes(addressAttr.getIp()));
        attrsByteBuf.writeBytes(attrByteBuf);
    }

    private void processChangeRequest(Channel channel, Map.Entry<AttrType, Attr> entry, ByteBuf attrsByteBuf) {
        AttrType key = entry.getKey();
        ChangeRequestAttr changeRequestAttr = (ChangeRequestAttr) entry.getValue();
        ByteBuf attrByteBuf = channel.alloc().heapBuffer();
        attrByteBuf.writeShort(key.getCode());
        attrByteBuf.writeShort(4);
        int flag = 0;
        if (changeRequestAttr.getChangeIP()) {
            flag |= 0b100;
        }
        if (changeRequestAttr.getChangePort()) {
            flag |= 0b10;
        }
        attrByteBuf.writeInt(flag);
        attrsByteBuf.writeBytes(attrByteBuf);
    }
}

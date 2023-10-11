package io.jaspercloud.sdwan;

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
        ByteBuf byteBuf = channel.alloc().buffer();
        ByteBuf attrsByteBuf = channel.alloc().buffer();
        try {
            for (Map.Entry<AttrType, Attr> entry : message.getAttrs().entrySet()) {
                AttrType key = entry.getKey();
                ByteBuf value = entry.getValue().toByteBuf();
                ByteBuf attrByteBuf = channel.alloc().buffer();
                try {
                    attrByteBuf.writeShort(key.getCode());
                    attrByteBuf.writeShort(value.readableBytes());
                    attrByteBuf.writeBytes(value);
                    attrsByteBuf.writeBytes(attrByteBuf);
                } finally {
                    value.release();
                    attrByteBuf.release();
                }
            }
            byteBuf.writeShort(message.getMessageType().getCode());
            byteBuf.writeShort(attrsByteBuf.readableBytes());
            byteBuf.writeBytes(StunMessage.Cookie);
            byteBuf.writeBytes(message.getTranId().getBytes());
            byteBuf.writeBytes(attrsByteBuf);
        } finally {
            attrsByteBuf.release();
        }
        DatagramPacket datagramPacket = new DatagramPacket(byteBuf, msg.recipient());
        out.add(datagramPacket);
    }
}

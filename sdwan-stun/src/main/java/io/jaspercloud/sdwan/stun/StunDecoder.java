package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.ByteBufUtil;
import io.jaspercloud.sdwan.IPUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.Arrays;
import java.util.List;

public class StunDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private List<AttrType> AddrTypeList = Arrays.asList(AttrType.MappedAddress, AttrType.OtherAddress, AttrType.ResponseOrigin);
    private List<AttrType> StringTypeList = Arrays.asList(AttrType.VIP, AttrType.EncryptKey, AttrType.ChannelId);

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        ByteBuf byteBuf = msg.content();
        int type = byteBuf.readUnsignedShort();
        int len = byteBuf.readUnsignedShort();
        //cookie
        byteBuf.skipBytes(StunMessage.Cookie.length);
        byte[] tranIdBytes = new byte[12];
        byteBuf.readBytes(tranIdBytes);
        String tranId = new String(tranIdBytes);
        StunMessage message = new StunMessage(MessageType.valueOf(type), tranId);
        ByteBuf attrs = byteBuf.readSlice(len);
        while (attrs.readableBytes() > 0) {
            int t = attrs.readUnsignedShort();
            int l = attrs.readUnsignedShort();
            ByteBuf v = attrs.readSlice(l);
            if (AddrTypeList.contains(AttrType.valueOf(t))) {
                int reserved = v.readUnsignedByte();
                int family = v.readUnsignedByte();
                int port = v.readUnsignedShort();
                byte[] bytes = new byte[4];
                v.readBytes(bytes);
                String ip = IPUtil.bytes2ip(bytes);
                Attr attr = new AddressAttr(ProtoFamily.valueOf(family), ip, port);
                message.getAttrs().put(AttrType.valueOf(t), attr);
            } else if (StringTypeList.contains(AttrType.valueOf(t))) {
                Attr attr = new StringAttr(new String(ByteBufUtil.toBytes(v)));
                message.getAttrs().put(AttrType.valueOf(t), attr);
            } else if (AttrType.Data.equals(AttrType.valueOf(t))) {
                Attr attr = new ByteBufAttr(v.retain());
                message.getAttrs().put(AttrType.valueOf(t), attr);
            }
        }
        StunPacket packet = new StunPacket(message, msg.recipient(), msg.sender());
        out.add(packet);
    }
}
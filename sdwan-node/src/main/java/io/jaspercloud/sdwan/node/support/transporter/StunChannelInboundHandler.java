//package io.jaspercloud.sdwan.node.support.transporter;
//
//import io.jaspercloud.sdwan.stun.MessageType;
//import io.jaspercloud.sdwan.stun.StunMessage;
//import io.jaspercloud.sdwan.stun.StunPacket;
//import io.netty.channel.SimpleChannelInboundHandler;
//
//public abstract class StunChannelInboundHandler extends SimpleChannelInboundHandler<StunPacket> {
//
//    private MessageType messageType;
//
//    public StunChannelInboundHandler(MessageType messageType) {
//        this.messageType = messageType;
//    }
//
//    @Override
//    public boolean acceptInboundMessage(Object msg) throws Exception {
//        boolean accept = super.acceptInboundMessage(msg);
//        if (accept) {
//            StunPacket packet = (StunPacket) msg;
//            StunMessage request = packet.content();
//            accept = messageType.equals(request.getMessageType());
//        }
//        return accept;
//    }
//}

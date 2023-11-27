package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.domain.relay.service.RelayService;
import io.jaspercloud.sdwan.stun.MessageType;
import io.jaspercloud.sdwan.stun.StunMessage;
import io.jaspercloud.sdwan.stun.StunPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

@Slf4j
@ChannelHandler.Sharable
public class RelayHandler extends SimpleChannelInboundHandler<StunPacket> {

    @Resource
    private RelayService relayService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
        StunMessage request = packet.content();
        InetSocketAddress sender = packet.sender();
        if (MessageType.BindRelayRequest.equals(request.getMessageType())) {
            StunPacket response = relayService.processBindRelay(packet);
            ctx.writeAndFlush(response);
        } else if (MessageType.Transfer.equals(request.getMessageType())) {
            StunPacket response = relayService.processTransfer(packet);
            if (null == response) {
                return;
            }
            ctx.writeAndFlush(response);
        } else {
            ctx.fireChannelRead(packet.retain());
        }
    }

}

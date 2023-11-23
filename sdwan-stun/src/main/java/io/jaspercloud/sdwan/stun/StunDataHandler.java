package io.jaspercloud.sdwan.stun;

import io.netty.channel.ChannelHandlerContext;

public interface StunDataHandler {

    void onData(ChannelHandlerContext ctx, StunPacket packet);
}

package io.jaspercloud.sdwan.node.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface SDWanDataHandler {

    void onData(ChannelHandlerContext ctx, SDWanProtos.Message msg);
}

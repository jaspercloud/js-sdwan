package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.app.SDWanControllerService;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class SDWanControllerProcessHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> {

    private SDWanControllerService controllerService;

    public SDWanControllerProcessHandler(SDWanControllerService controllerService) {
        this.controllerService = controllerService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
        Channel channel = ctx.channel();
        switch (request.getType().getNumber()) {
            case SDWanProtos.MsgTypeCode.HeartType_VALUE: {
                processHeart(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.RegReqType_VALUE: {
                controllerService.regist(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.SDArpReqType_VALUE: {
                processSDArp(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.PunchingType_VALUE: {
                processPunching(channel, request);
                break;
            }
        }
    }

    private void processPunching(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.Punching punching = SDWanProtos.Punching.parseFrom(request.getData());
        String dstVIP = punching.getDstVIP();
        Channel targetChannel = controllerService.findNodeByIP(dstVIP);
        if (null == targetChannel) {
            return;
        }
        targetChannel.writeAndFlush(request);
    }

    private void processSDArp(Channel channel, SDWanProtos.Message request) throws Exception {
        SDWanProtos.SDArpResp sdArpResp = controllerService.sdArp(channel, request);
        SDWanProtos.Message response = request.toBuilder()
                .setType(SDWanProtos.MsgTypeCode.SDArpRespType)
                .setData(sdArpResp.toByteString())
                .build();
        channel.writeAndFlush(response);
    }

    private void processHeart(Channel channel, SDWanProtos.Message request) {
        channel.writeAndFlush(request);
    }
}

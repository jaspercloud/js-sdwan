package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.domain.control.service.SDWanControllerService;
import io.jaspercloud.sdwan.domain.control.service.SDWanSignalService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ControlHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> {

    private SDWanControllerService controllerService;
    private SDWanSignalService sdWanSignalService;

    public ControlHandler(SDWanControllerService controllerService,
                          SDWanSignalService sdWanSignalService) {
        this.controllerService = controllerService;
        this.sdWanSignalService = sdWanSignalService;
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
            case SDWanProtos.MsgTypeCode.RouteListReqType_VALUE: {
                controllerService.processRouteList(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.NodeInfoReqType_VALUE: {
                controllerService.processNodeInfo(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.P2pOfferType_VALUE: {
                sdWanSignalService.processP2pOffer(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.P2pAnswerType_VALUE: {
                sdWanSignalService.processP2pAnswer(channel, request);
                break;
            }
        }
    }

    private void processHeart(Channel channel, SDWanProtos.Message request) {
        channel.writeAndFlush(request);
    }
}

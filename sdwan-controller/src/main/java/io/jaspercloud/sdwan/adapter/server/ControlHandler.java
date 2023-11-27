package io.jaspercloud.sdwan.adapter.server;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.domain.control.service.SDWanControlService;
import io.jaspercloud.sdwan.domain.control.service.SDWanSignalService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ControlHandler extends SimpleChannelInboundHandler<SDWanProtos.Message> {

    private SDWanControlService controlService;
    private SDWanSignalService sdWanSignalService;

    public ControlHandler(SDWanControlService controlService,
                          SDWanSignalService sdWanSignalService) {
        this.controlService = controlService;
        this.sdWanSignalService = sdWanSignalService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message request) throws Exception {
        Channel channel = ctx.channel();
        switch (request.getType().getNumber()) {
            case SDWanProtos.MsgTypeCode.HeartType_VALUE: {
                controlService.processHeart(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.RegReqType_VALUE: {
                controlService.regist(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.RouteListReqType_VALUE: {
                controlService.processRouteList(channel, request);
                break;
            }
            case SDWanProtos.MsgTypeCode.NodeInfoReqType_VALUE: {
                controlService.processNodeInfo(channel, request);
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

}

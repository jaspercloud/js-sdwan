package io.jaspercloud.sdwan.domain.control.service.impl;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.domain.control.service.SDWanNodeManager;
import io.jaspercloud.sdwan.domain.control.service.SDWanSignalService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class SDWanSignalServiceImpl implements SDWanSignalService {

    @Resource
    private SDWanNodeManager sdWanNodeManager;

    @Override
    public void processP2pOffer(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(request.getData());
            Channel targetChannel = sdWanNodeManager.getChannel(p2pOffer.getDstVIP());
            if (null == targetChannel) {
                log.debug("processP2pOffer not found channel: vip={}", p2pOffer.getDstVIP());
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setCode(SDWanProtos.MessageCode.NotFound_VALUE)
                        .build();
                SDWanProtos.Message resp = request.toBuilder()
                        .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                        .setData(p2pAnswer.toByteString())
                        .build();
                channel.writeAndFlush(resp);
                return;
            }
            targetChannel.writeAndFlush(request);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError_VALUE)
                    .build();
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                    .setData(p2pAnswer.toByteString())
                    .build();
            channel.writeAndFlush(resp);
        }
    }

    @Override
    public void processP2pAnswer(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.parseFrom(request.getData());
            Channel targetChannel = sdWanNodeManager.getChannel(p2pAnswer.getDstVIP());
            if (null == targetChannel) {
                log.error("processP2pAnswer not found channel: vip={}", p2pAnswer.getDstVIP());
                return;
            }
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                    .setData(p2pAnswer.toByteString())
                    .build();
            targetChannel.writeAndFlush(resp);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError_VALUE)
                    .build();
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                    .setData(p2pAnswer.toByteString())
                    .build();
            channel.writeAndFlush(resp);
        }
    }
}

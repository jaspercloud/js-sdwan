package io.jaspercloud.sdwan.service;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class SDWanSignalService {

    @Resource
    private NodeManager nodeManager;

    public void processP2pOffer(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(request.getData());
            Channel targetChannel = nodeManager.getChannel(p2pOffer.getDstVIP());
            if (null == targetChannel) {
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
        } catch (Exception e) {
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

    public void processP2pAnswer(Channel channel, SDWanProtos.Message request) {
        try {
            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.parseFrom(request.getData());
            Channel targetChannel = nodeManager.getChannel(p2pAnswer.getDstVIP());
            if (null == targetChannel) {
                return;
            }
            SDWanProtos.Message resp = request.toBuilder()
                    .setType(SDWanProtos.MsgTypeCode.P2pAnswerType)
                    .setData(p2pAnswer.toByteString())
                    .build();
            targetChannel.writeAndFlush(resp);
        } catch (Exception e) {
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

package io.jaspercloud.sdwan.domain.control.service;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;

public interface SDWanSignalService {

    void processP2pOffer(Channel channel, SDWanProtos.Message request);

    void processP2pAnswer(Channel channel, SDWanProtos.Message request);

}

package io.jaspercloud.sdwan.domain.control.service;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.Channel;

public interface SDWanControllerService {

    void regist(Channel channel, SDWanProtos.Message request);

    void processRouteList(Channel channel, SDWanProtos.Message request);

    void processNodeInfo(Channel channel, SDWanProtos.Message request);

}

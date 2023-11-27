package io.jaspercloud.sdwan.domain.relay.service;

import io.jaspercloud.sdwan.stun.StunPacket;
import io.netty.channel.Channel;

public interface RelayService {

    void processBindRelay(Channel channel, StunPacket packet);

    void processTransfer(Channel channel, StunPacket packet);

}

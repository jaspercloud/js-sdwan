package io.jaspercloud.sdwan.domain.relay.service;

import io.jaspercloud.sdwan.stun.StunPacket;

public interface RelayService {

    StunPacket processBindRelay(StunPacket packet);

    StunPacket processTransfer(StunPacket packet);

}

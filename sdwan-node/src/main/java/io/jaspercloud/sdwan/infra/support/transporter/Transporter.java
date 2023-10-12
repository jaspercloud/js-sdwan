package io.jaspercloud.sdwan.infra.support.transporter;

import io.jaspercloud.sdwan.tun.TunChannel;
import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

public interface Transporter {

    void bind(TunChannel tunChannel);

    interface Filter {

        ByteBuf encode(InetSocketAddress address, ByteBuf byteBuf);

        ByteBuf decode(InetSocketAddress address, ByteBuf byteBuf);
    }
}

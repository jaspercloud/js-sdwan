package io.jasercloud.sdwan.support.transporter;

import io.jasercloud.sdwan.tun.TunChannel;

public interface Transporter {

    void bind(TunChannel tunChannel);
}

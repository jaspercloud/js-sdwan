package io.jaspercloud.sdwan.stun;

import io.netty.buffer.ByteBuf;

public interface AttrDecode {

    Attr decode(ByteBuf byteBuf);
}

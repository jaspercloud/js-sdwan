package io.jaspercloud.sdwan.stun;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public abstract class Attr {

    public abstract void write(ByteBuf byteBuf);
}
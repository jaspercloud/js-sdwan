package io.jasercloud.sdwan;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DataAttr extends Attr {

    private ByteBuf byteBuf;
}

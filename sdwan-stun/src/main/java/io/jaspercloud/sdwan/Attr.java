package io.jaspercloud.sdwan;

import io.netty.util.ReferenceCounted;
import lombok.Data;

@Data
public class Attr implements Referenced {

    @Override
    public ReferenceCounted retain(int increment) {
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}
package io.jasercloud.sdwan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StringAttr extends Attr {

    private String data;

    @Override
    public StringAttr retain(int increment) {
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}

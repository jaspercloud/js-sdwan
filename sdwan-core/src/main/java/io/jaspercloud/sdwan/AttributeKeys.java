package io.jaspercloud.sdwan;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class AttributeKeys {

    private AttributeKeys() {

    }

    public static Attribute<String> nodeName(Channel channel) {
        Attribute<String> attr = channel.attr(AttributeKey.valueOf("nodeName"));
        return attr;
    }

    public static Attribute<String> vip(Channel channel) {
        Attribute<String> attr = channel.attr(AttributeKey.valueOf("vip"));
        return attr;
    }
}

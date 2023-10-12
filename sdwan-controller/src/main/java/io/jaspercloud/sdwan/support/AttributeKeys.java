package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.domian.Node;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class AttributeKeys {

    private AttributeKeys() {

    }

    public static Attribute<Node> node(Channel channel) {
        Attribute<Node> attr = channel.attr(AttributeKey.valueOf("node"));
        return attr;
    }
}

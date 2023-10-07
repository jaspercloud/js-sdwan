package io.jasercloud.sdwan.support;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class AttributeKeys {

    private AttributeKeys() {

    }

    public static Attribute<NodeInfo> nodeInfo(Channel channel) {
        Attribute<NodeInfo> attr = channel.attr(AttributeKey.valueOf("nodeInfo"));
        return attr;
    }
}

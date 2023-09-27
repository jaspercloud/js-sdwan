package io.jaspercloud.sdwan;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class AttributeKeys {

    private AttributeKeys() {

    }

    public static Attribute<InetSocketAddress> nodePublicAddress(Channel channel) {
        Attribute<InetSocketAddress> attr = channel.attr(AttributeKey.valueOf("nodePublicAddress"));
        return attr;
    }

    public static Attribute<String> nodeHardwareAddress(Channel channel) {
        Attribute<String> attr = channel.attr(AttributeKey.valueOf("nodeHardwareAddress"));
        return attr;
    }

    public static Attribute<String> nodeVip(Channel channel) {
        Attribute<String> attr = channel.attr(AttributeKey.valueOf("vip"));
        return attr;
    }

    public static Attribute<Cidr> nodeCidr(Channel channel) {
        Attribute<Cidr> attr = channel.attr(AttributeKey.valueOf("meshCidr"));
        return attr;
    }
}

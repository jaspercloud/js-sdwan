package io.jasercloud.sdwan.tun;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;

public class TunChannelConfig extends DefaultChannelConfig {

    public static final ChannelOption<Integer> MTU = ChannelOption.valueOf("MTU");

    private Integer mtu = 1500;

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public TunChannelConfig(Channel channel) {
        super(channel);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        if (MTU.equals(option)) {
            setMtu((Integer) value);
            return true;
        } else {
            return super.setOption(option, value);
        }
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (MTU.equals(option)) {
            return (T) getMtu();
        } else {
            return super.getOption(option);
        }
    }
}

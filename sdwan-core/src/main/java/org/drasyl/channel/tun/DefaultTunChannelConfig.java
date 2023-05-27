/*
 * Copyright (c) 2021-2022 Heiko Bornholdt and Kevin Röbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.drasyl.channel.tun;

import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;

import static org.drasyl.channel.tun.TunChannelOption.TUN_MTU;

/**
 * The default {@link TunChannelConfig} implementation.
 */
public class DefaultTunChannelConfig extends DefaultChannelConfig implements TunChannelConfig {
    private int mtu;

    public DefaultTunChannelConfig(final TunChannel channel) {
        super(channel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(final ChannelOption<T> option) {
        if (option == TUN_MTU) {
            return (T) Integer.valueOf(getMtu());
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(final ChannelOption<T> option, final T value) {
        if (!super.setOption(option, value)) {
            if (option == TUN_MTU) {
                setMtu((Integer) value);
            }
            else {
                return false;
            }
        }

        return true;
    }

    @Override
    public int getMtu() {
        return mtu;
    }

    @Override
    public TunChannelConfig setMtu(final int mtu) {
        if (mtu < 0) {
            throw new IllegalArgumentException("mtu must be non-negative.");
        }
        this.mtu = mtu;
        return null;
    }
}

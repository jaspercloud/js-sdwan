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

import io.netty.channel.ChannelConfig;

/**
 * A {@link ChannelConfig} for a {@link TunChannel}.
 *
 * <h3>Available options</h3>
 * <p>
 * In addition to the options provided by {@link ChannelConfig}, {@link TunChannelConfig} allows the
 * following options in the option map:
 *
 * <table border="1" cellspacing="0" cellpadding="6">
 * <tr>
 * <th>Name</th><th>Associated setter method</th>
 * </tr><tr>
 * <td>{@link TunChannelOption#TUN_MTU}</td><td>{@link #setMtu(int)}</td>
 * </tr>
 * </table>
 */
public interface TunChannelConfig extends ChannelConfig {
    /**
     * Gets the {@link TunChannelOption#TUN_MTU} option.
     */
    int getMtu();

    /**
     * Sets the {@link TunChannelOption#TUN_MTU} option.
     */
    TunChannelConfig setMtu(int mtu);
}

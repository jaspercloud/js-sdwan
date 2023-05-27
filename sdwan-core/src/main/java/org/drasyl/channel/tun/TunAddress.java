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

import org.apache.commons.lang3.StringUtils;

import java.net.SocketAddress;
import java.util.Objects;

/**
 * A {@link SocketAddress} implementation that identifies a tun device to which a {@link TunChannel}
 * can be bound to.
 */
public class TunAddress extends SocketAddress {
    private static final long serialVersionUID = -584786182484350484L; // NOSONAR
    private final String ifName;

    public TunAddress(final String ifName) {
        this.ifName = ifName;
    }

    public TunAddress() {
        this(null);
    }

    /**
     * Returns the name of the tun device.
     *
     * @return the name of the tun device
     */
    public String ifName() {
        return ifName;
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(ifName)) {
            return ifName;

        }
        return "";
    }
}

/*
 * Copyright (c) 2021 Heiko Bornholdt and Kevin Röbert
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
package org.drasyl.channel.tun.jna.shared;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * JNA mapping for <a href="https://opensource.apple.com/source/xnu/xnu-344/bsd/net/if.h.auto.html">if.h</a>.
 */
@SuppressWarnings("java:S2974")
public final class If {
    public static final int IFNAMSIZ = 16;

    private If() {
        // JNA mapping
    }

    @SuppressWarnings({ "java:S116", "java:S1104", "java:S2160" })
    @FieldOrder({ "ifr_name", "ifr_ifru" })
    public static class Ifreq extends Structure {
        public byte[] ifr_name;
        public FfrIfru ifr_ifru;

        public Ifreq(final String name) {
            this.ifr_name = new byte[IFNAMSIZ];
            if (name != null) {
                final byte[] bytes = name.getBytes(US_ASCII);
                System.arraycopy(bytes, 0, this.ifr_name, 0, bytes.length);
            }
        }

        public Ifreq(final String ifr_name, final short flags) {
            this.ifr_name = new byte[IFNAMSIZ];
            if (ifr_name != null) {
                final byte[] bytes = ifr_name.getBytes(US_ASCII);
                System.arraycopy(bytes, 0, this.ifr_name, 0, bytes.length);
            }
            this.ifr_ifru.setType("ifru_flags");
            this.ifr_ifru.ifru_flags = flags;
        }

        public Ifreq(final String ifr_name, final int mtu) {
            this.ifr_name = new byte[IFNAMSIZ];
            if (ifr_name != null) {
                final byte[] bytes = ifr_name.getBytes(US_ASCII);
                System.arraycopy(bytes, 0, this.ifr_name, 0, bytes.length);
            }
            this.ifr_ifru.setType("ifru_mtu");
            this.ifr_ifru.ifru_mtu = mtu;
        }

        @SuppressWarnings({ "java:S116", "java:S1104", "java:S2160" })
        public static class FfrIfru extends Union {
            public short ifru_flags;
            public int ifru_mtu;
        }
    }
}

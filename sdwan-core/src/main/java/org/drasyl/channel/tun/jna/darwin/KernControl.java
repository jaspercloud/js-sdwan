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
package org.drasyl.channel.tun.jna.darwin;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * JNA mapping for <a href="https://opensource.apple.com/source/xnu/xnu-6153.41.3/bsd/sys/kern_control.h.auto.html">kern_control.h</a>.
 */
final class KernControl {
    static final NativeLong CTLIOCGINFO = new NativeLong(0xc0644e03L);
    public static final int MAX_KCTL_NAME = 96;

    private KernControl() {
        // JNA mapping
    }

    @SuppressWarnings({ "java:S116", "java:S1104", "java:S2160" })
    @FieldOrder({ "ctl_id", "ctl_name" })
    public static class CtlInfo extends Structure {
        public int ctl_id;
        public byte[] ctl_name;

        public CtlInfo(final String name) {
            ctl_name = new byte[MAX_KCTL_NAME];
            final byte[] bytes = name.getBytes(US_ASCII);
            System.arraycopy(bytes, 0, ctl_name, 0, bytes.length);
        }
    }

    @SuppressWarnings({ "java:S109", "java:S116", "java:S1104", "java:S2160" })
    @FieldOrder({ "sc_len", "sc_family", "ss_sysaddr", "sc_reserved" })
    public static class SockaddrCtl extends Structure {
        public byte sc_len = 32;
        public byte sc_family;
        public short ss_sysaddr;
        public int[] sc_reserved = new int[7];

        public SockaddrCtl(final int addressFamily, final short sysaddr, final int... reserved) {
            sc_family = (byte) addressFamily;
            ss_sysaddr = sysaddr;
            System.arraycopy(reserved, 0, sc_reserved, 0, reserved.length);
        }
    }
}

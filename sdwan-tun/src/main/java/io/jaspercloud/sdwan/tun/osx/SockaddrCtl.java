package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.Structure;

@Structure.FieldOrder({"sc_len", "sc_family", "ss_sysaddr", "sc_reserved"})
public class SockaddrCtl extends Structure {

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
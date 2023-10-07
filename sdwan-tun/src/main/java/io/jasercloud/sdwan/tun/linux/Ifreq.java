package io.jasercloud.sdwan.tun.linux;

import com.sun.jna.Structure;
import com.sun.jna.Union;

import static java.nio.charset.StandardCharsets.US_ASCII;

@Structure.FieldOrder({"ifr_name", "ifr_ifru"})
public class Ifreq extends Structure {

    public static final int IFNAMSIZ = 16;

    public byte[] ifr_name;
    public LinuxTunDevice.LinuxC.Ifreq.FfrIfru ifr_ifru;

    public Ifreq(final String ifr_name, final short flags) {
        this.ifr_name = new byte[IFNAMSIZ];
        if (ifr_name != null) {
            final byte[] bytes = ifr_name.getBytes(US_ASCII);
            System.arraycopy(bytes, 0, this.ifr_name, 0, bytes.length);
        }
        this.ifr_ifru.setType("ifru_flags");
        this.ifr_ifru.ifru_flags = flags;
    }

    public static class FfrIfru extends Union {

        public short ifru_flags;
    }
}
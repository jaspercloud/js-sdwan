package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.Structure;

import static java.nio.charset.StandardCharsets.US_ASCII;

@Structure.FieldOrder({"ctl_id", "ctl_name"})
public class CtlInfo extends Structure {

    public static final int MAX_KCTL_NAME = 96;

    public int ctl_id;
    public byte[] ctl_name;

    public CtlInfo(final String name) {
        ctl_name = new byte[MAX_KCTL_NAME];
        final byte[] bytes = name.getBytes(US_ASCII);
        System.arraycopy(bytes, 0, ctl_name, 0, bytes.length);
    }
}
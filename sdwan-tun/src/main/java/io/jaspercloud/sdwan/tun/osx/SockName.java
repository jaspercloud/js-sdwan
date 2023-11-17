package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.Structure;

@Structure.FieldOrder({"name"})
public class SockName extends Structure {

    public static final int LENGTH = 16;
    public byte[] name = new byte[LENGTH];
}
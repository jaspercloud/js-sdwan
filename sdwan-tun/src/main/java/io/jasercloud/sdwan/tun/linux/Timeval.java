package io.jasercloud.sdwan.tun.linux;

import com.sun.jna.Structure;

@Structure.FieldOrder({"tv_sec", "tv_usec"})
public class Timeval extends Structure {

    public long tv_sec;
    public long tv_usec;
}
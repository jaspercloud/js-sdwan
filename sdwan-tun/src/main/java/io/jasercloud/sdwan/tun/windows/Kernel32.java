package io.jasercloud.sdwan.tun.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

public interface Kernel32 extends StdCallLibrary {

    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

    int INFINITE = 0xFFFFFFFF;

    int WaitForSingleObject(Pointer hHandle, int dwMilliseconds);
}
package io.jasercloud.sdwan.tun.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

public interface NativeKernel32Api extends StdCallLibrary {

    NativeKernel32Api INSTANCE = Native.load("kernel32", NativeKernel32Api.class);

    int INFINITE = 0xFFFFFFFF;

    int WaitForSingleObject(Pointer hHandle, int dwMilliseconds);
}
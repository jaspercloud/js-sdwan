/* Copyright (c) 2010 Daniel Doubrovkine, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package org.drasyl.channel.tun.jna.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.drasyl.channel.tun.jna.windows.WinNT.HANDLE;

/**
 * Ported from Winbase.h (kernel32.dll/kernel services). Microsoft Windows SDK 6.0A.
 *
 * @author dblock[at]dblock.org
 */
@SuppressWarnings("java:S1214")
public interface WinBase extends WinDef {
    /**
     * Constant value representing an invalid HANDLE.
     */
    HANDLE INVALID_HANDLE_VALUE =
            new HANDLE(Pointer.createConstant(Native.POINTER_SIZE == 8
                    ? -1 : 0xFFFFFFFFL));
    int INFINITE = 0xFFFFFFFF;
}

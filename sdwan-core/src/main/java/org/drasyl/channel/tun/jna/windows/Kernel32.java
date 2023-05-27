/* Copyright (c) 2007, 2013 Timothy Wall, Markus Karg, All Rights Reserved
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
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.drasyl.channel.tun.jna.windows.WinNT.HANDLE;

/**
 * Interface definitions for <code>kernel32.dll</code>. Includes additional alternate mappings from
 * {@link WinNT} which make use of NIO buffers, {@link Wincon} for console API.
 */
@SuppressWarnings("java:S100")
public interface Kernel32 extends StdCallLibrary {
    /**
     * The instance.
     */
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * Waits until the specified object is in the signaled state or the time-out interval elapses.
     * To enter an alertable wait state, use the WaitForSingleObjectEx function. To wait for
     * multiple objects, use the WaitForMultipleObjects.
     *
     * @param hHandle        A handle to the object. For a list of the object types whose handles
     *                       can be specified, see the following Remarks section. If this handle is
     *                       closed while the wait is still pending, the function's behavior is
     *                       undefined. The handle must have the SYNCHRONIZE access right. For more
     *                       information, see Standard Access Rights.
     * @param dwMilliseconds The time-out interval, in milliseconds. If a nonzero value is
     *                       specified, the function waits until the object is signaled or the
     *                       interval elapses. If dwMilliseconds is zero, the function does not
     *                       enter a wait state if the object is not signaled; it always returns
     *                       immediately. If dwMilliseconds is INFINITE, the function will return
     *                       only when the object is signaled.
     * @return If the function succeeds, the return value indicates the event that caused the
     * function to return.
     */
    int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds);
}

/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
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

import com.sun.jna.FromNativeContext;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * This module defines the 32-Bit Windows types and constants that are defined by NT, but exposed
 * through the Win32 API. Ported from WinNT.h Microsoft Windows SDK 6.0A. Avoid including any NIO
 * Buffer mappings here; put them in a DLL-derived interface (e.g. kernel32, user32, etc) instead.
 *
 * @author dblock[at]dblock.org
 */
@SuppressWarnings("serial")
public interface WinNT {
    @SuppressWarnings("java:S2160")
    class HANDLE extends PointerType {
        private boolean immutable;

        public HANDLE() {
        }

        public HANDLE(final Pointer p) {
            setPointer(p);
            immutable = true;
        }

        /**
         * Override to the appropriate object for INVALID_HANDLE_VALUE.
         */
        @Override
        public Object fromNative(final Object nativeValue, final FromNativeContext context) {
            final Object o = super.fromNative(nativeValue, context);
            if (WinBase.INVALID_HANDLE_VALUE.equals(o)) {
                return WinBase.INVALID_HANDLE_VALUE;
            }
            return o;
        }

        @Override
        public void setPointer(final Pointer p) {
            if (immutable) {
                throw new UnsupportedOperationException("immutable reference");
            }

            super.setPointer(p);
        }

        @Override
        public String toString() {
            return String.valueOf(getPointer());
        }
    }
}

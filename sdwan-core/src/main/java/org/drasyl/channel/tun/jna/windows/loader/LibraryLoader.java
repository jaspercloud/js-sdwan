/*
 * Copyright (c) 2021 Heiko Bornholdt and Kevin Röbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.drasyl.channel.tun.jna.windows.loader;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Helper class to load the library from the preferred location.
 */
public class LibraryLoader {
    public static final String PREFER_SYSTEM = "pref_system";
    public static final String PREFER_BUNDLED = "pref_bundled";
    public static final String BUNDLED_ONLY = "bundled_only";
    public static final String SYSTEM_ONLY = "system_only";
    private final Class clazz;

    public LibraryLoader(final Class classToRegister) {
        clazz = requireNonNull(classToRegister);
    }

    public static String getPlatformDependentPath(final String libFullName) {
        final boolean is64Bit = Native.POINTER_SIZE == 8;
        if (Platform.isWindows()) {
            if (Platform.isARM()) {
                if (is64Bit) {
                    return getPath("arm64", libFullName);
                }
                return getPath("arm", libFullName);
            }
            else {
                if (is64Bit) {
                    return getPath("amd64", libFullName);
                }
                return getPath("x86", libFullName);
            }
        }

        return null;
    }

    public static String getPathInResources(final String name) throws IOException {
        final String platformPath = getPlatformDependentPath(name + ".dll");

        if (platformPath == null) {
            final String message = String.format("Unsupported platform: %s/%s", System.getProperty("os.name"),
                    System.getProperty("os.arch"));
            throw new IOException(message);
        }

        return getPath("/META-INF", "native", name, platformPath);
    }

    private static String getPath(final String... parts) {
        final String separator = "/";
        return String.join(separator, parts);
    }

    public void loadSystemLibrary(final String library) throws IOException {
        try {
            Native.register(clazz, library);
        }
        catch (final UnsatisfiedLinkError e) {
            throw new IOException(e);
        }
    }

    private void loadBundledLibrary(final String name) throws IOException {
        final String pathInJar = LibraryLoader.getPathInResources(name);

        try {
            NativeLoader.loadLibraryFromJar(pathInJar, clazz);
        }
        catch (final UnsatisfiedLinkError e) {
            throw new IOException("Could not load lib from " + pathInJar, e);
        }
    }

    public void loadLibrary(final String mode,
                            final String systemFallBack) throws IOException {
        switch (mode) {
            case PREFER_SYSTEM:
                try {
                    loadSystemLibrary(systemFallBack);
                }
                catch (final IOException suppressed) {
                    // Attempt to load the bundled
                    loadBundledLibrary(systemFallBack);
                }
                break;
            case PREFER_BUNDLED:
                try {
                    loadBundledLibrary(systemFallBack);
                }
                catch (final IOException suppressed) {
                    loadSystemLibrary(systemFallBack);
                }
                break;
            case BUNDLED_ONLY:
                loadBundledLibrary(systemFallBack);
                break;
            case SYSTEM_ONLY:
                loadSystemLibrary(systemFallBack);
                break;
            default:
                throw new IOException("Unsupported mode: " + mode);
        }
    }
}

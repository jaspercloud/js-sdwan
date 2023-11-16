package io.jaspercloud.sdwan.tun;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.linux.NativeLinuxApi;
import io.jaspercloud.sdwan.tun.osx.NativeOsxApi;
import io.netty.util.internal.PlatformDependent;

public class CheckAdmin {

    public static void check() {
        boolean isAdmin;
        if (PlatformDependent.isOsx()) {
            isAdmin = checkOsx();
        } else if (PlatformDependent.isWindows()) {
            isAdmin = checkWindows();
        } else {
            isAdmin = checkLinux();
        }
        if (!isAdmin) {
            throw new ProcessException("请以管理员权限运行");
        }
    }

    private static boolean checkWindows() {
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.GetCurrentProcess();
        WinNT.HANDLEByReference phToken = new WinNT.HANDLEByReference();
        if (!Advapi32.INSTANCE.OpenProcessToken(processHandle, WinNT.TOKEN_QUERY, phToken)) {
            throw new ProcessException("OpenProcessToken error");
        }
        WinNT.TOKEN_ELEVATION elevation = new WinNT.TOKEN_ELEVATION();
        IntByReference tokenInformationLength = new IntByReference();
        if (!Advapi32.INSTANCE.GetTokenInformation(phToken.getValue(),
                WinNT.TOKEN_INFORMATION_CLASS.TokenElevation,
                elevation, elevation.size(),
                tokenInformationLength)) {
            throw new ProcessException("GetTokenInformation error");
        }
        return 1 == elevation.TokenIsElevated;
    }

    private static boolean checkOsx() {
        return 0 == NativeOsxApi.geteuid();
    }

    private static boolean checkLinux() {
        return 0 == NativeLinuxApi.geteuid();
    }
}

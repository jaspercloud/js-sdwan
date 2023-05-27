package demo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WinTunManager {

    private static Map<String, WinTun> winTunMap = new ConcurrentHashMap<>();

    public WinTunManager() {

    }

    public static void addWinTun(String ip, WinTun winTun) {
        winTunMap.put(ip, winTun);
    }

    public static WinTun getWinTun(String ip) {
        return winTunMap.get(ip);
    }
}

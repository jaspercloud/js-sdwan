package io.jasercloud.sdwan.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemberNodeManager {

    private Map<String, String> nodeMap = new ConcurrentHashMap<>();

    public void add(String nodeName, String vip) {
        nodeMap.put(nodeName, vip);
    }
}

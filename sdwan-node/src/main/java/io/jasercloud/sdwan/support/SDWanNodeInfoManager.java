package io.jasercloud.sdwan.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SDWanNodeInfoManager {

    private Map<String, SDWanNodeInfo> nodeMap = new ConcurrentHashMap<>();

    public void add(SDWanNodeInfo node) {
        nodeMap.put(node.getVip(), node);
    }

    public SDWanNodeInfo get(String vip) {
        return nodeMap.get(vip);
    }
}

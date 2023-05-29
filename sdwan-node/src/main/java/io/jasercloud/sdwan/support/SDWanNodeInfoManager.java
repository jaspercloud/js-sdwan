package io.jasercloud.sdwan.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SDWanNodeInfoManager {

    private Map<String, SDWanNodeInfo> nodeMap = new ConcurrentHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public void updateList(List<SDWanNodeInfo> list) {
        lock.writeLock().lock();
        try {
            nodeMap.clear();
            for (SDWanNodeInfo node : list) {
                nodeMap.put(node.getVip(), node);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void add(SDWanNodeInfo node) {
        lock.readLock().lock();
        try {
            nodeMap.put(node.getVip(), node);
        } finally {
            lock.readLock().unlock();
        }
    }

    public SDWanNodeInfo get(String vip) {
        lock.readLock().lock();
        try {
            return nodeMap.get(vip);
        } finally {
            lock.readLock().unlock();
        }
    }
}

package io.jasercloud.sdwan.support;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AsyncTask<T> {

    private static Map<String, CompletableFuture> futureMap = new ConcurrentHashMap<>();

    public static <T> T waitTask(String id, CompletableFuture<T> future, long timeout) throws Exception {
        futureMap.put(id, future);
        try {
            T result = future.get(timeout, TimeUnit.MILLISECONDS);
            return result;
        } finally {
            futureMap.remove(id);
        }
    }

    public static void delTask(String id) {
        futureMap.remove(id);
    }

    public static void completeTask(String id, Object result) {
        CompletableFuture future = futureMap.get(id);
        if (null != future) {
            future.complete(result);
        }
    }
}

package io.jasercloud.sdwan.support;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncTask<T> {

    public static final Timer TIMEOUT = new HashedWheelTimer(
            new DefaultThreadFactory("async-future-timeout", true),
            30, TimeUnit.MILLISECONDS);
    private static Map<String, CompletableFuture> futureMap = new ConcurrentHashMap<>();

    public static <T> T waitSync(String id, long timeout) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        futureMap.put(id, future);
        try {
            T result = future.get(timeout, TimeUnit.MILLISECONDS);
            return result;
        } finally {
            futureMap.remove(id);
        }
    }

    public static <T> CompletableFuture<T> waitAsync(String id, long timeout) {
        FutureTask futureTask = new FutureTask();
        futureMap.put(id, futureTask);
        futureTask.timeoutTask = TIMEOUT.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                completeExceptionallyTask(id, new TimeoutException());
            }
        }, timeout, TimeUnit.MILLISECONDS);
        return futureTask;
    }

    public static void completeTask(String id, Object result) {
        CompletableFuture future = futureMap.remove(id);
        if (null != future && !future.isDone()) {
            future.complete(result);
        }
    }

    public static void completeExceptionallyTask(String id, Throwable throwable) {
        CompletableFuture future = futureMap.remove(id);
        if (null != future && !future.isDone()) {
            future.completeExceptionally(throwable);
        }
    }

    private static class FutureTask extends CompletableFuture {

        private Timeout timeoutTask;

        @Override
        public boolean complete(Object value) {
            timeoutTask.cancel();
            return super.complete(value);
        }
    }
}

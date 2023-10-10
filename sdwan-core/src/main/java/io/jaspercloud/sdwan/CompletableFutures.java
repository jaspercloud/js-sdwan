package io.jaspercloud.sdwan;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public final class CompletableFutures {

    private CompletableFutures() {

    }

    public static <T> CompletableFuture<T> order(CompletableFuture<T>... futures) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Iterator<CompletableFuture<T>> iterator = Arrays.asList(futures).iterator();
        if (iterator.hasNext()) {
            doNext(iterator, iterator.next(), result);
        }
        return result;
    }

    private static <T> void doNext(Iterator<CompletableFuture<T>> iterator, CompletableFuture<T> next, CompletableFuture<T> result) {
        next.whenComplete((data, throwable) -> {
            if (null != throwable) {
                if (!iterator.hasNext()) {
                    result.completeExceptionally(throwable);
                    return;
                }
                doNext(iterator, iterator.next(), result);
                return;
            }
            result.complete(data);
        });
    }
}

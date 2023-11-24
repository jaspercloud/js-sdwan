package io.jaspercloud.sdwan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CompletableFutures {

    private CompletableFutures() {

    }

    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> list) {
        CompletableFuture<Void> future = CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));
        CompletableFuture<List<T>> result = new CompletableFuture<>();
        future.whenComplete((r, e) -> {
            List<T> resultList = new ArrayList<>();
            for (CompletableFuture<T> f : list) {
                try {
                    T item = f.get();
                    resultList.add(item);
                } catch (Throwable ex) {
                }
            }
            result.complete(resultList);
        });
        return result;
    }

    public static <T> CompletableFuture<T> onException(CompletableFuture<T> last, Supplier<CompletableFuture<T>> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        last.whenComplete((result1, error1) -> {
            if (null == error1) {
                future.complete(result1);
                return;
            }
            try {
                supplier.get().whenComplete((result2, error2) -> {
                    if (null == error2) {
                        future.complete(result2);
                        return;
                    }
                    future.completeExceptionally(error2);
                });
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
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

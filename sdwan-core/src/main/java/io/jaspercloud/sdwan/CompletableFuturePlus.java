package io.jaspercloud.sdwan;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CompletableFuturePlus {

    private CompletableFuturePlus() {

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
}

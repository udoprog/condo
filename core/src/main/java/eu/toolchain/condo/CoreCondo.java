package eu.toolchain.condo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Core implementation of Condo.
 */
@RequiredArgsConstructor
public class CoreCondo<M> implements Condo<M> {
  private final Executor executor;

  /**
   * Coordination lock for this instance.
   */
  private final Object processedLock = new Object();
  private final Object maskLock = new Object();

  private final List<Predicate<M>> masks = new ArrayList<>();
  private final List<DeferredAction<M>> deferred = new ArrayList<>();
  private final List<M> processed = new ArrayList<>();
  private final List<M> onceProcessed = new ArrayList<>();

  @Override
  public <T> CompletableFuture<T> schedule(
      final M metadata, final Supplier<T> action
  ) {
    return scheduleAsync(metadata, () -> CompletableFuture.supplyAsync(action, executor));
  }

  @Override
  public <T> CompletableFuture<T> scheduleAsync(
      final M metadata, final Supplier<? extends CompletionStage<T>> action
  ) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    final DeferredAction<M> deferred = deferAction(metadata, action, future);

    synchronized (maskLock) {
      if (masks.stream().anyMatch(p -> p.test(metadata))) {
        this.deferred.add(deferred);
        maskLock.notifyAll();
        return future;
      }
    }

    deferred.runnable.run();
    return future;
  }

  @Override
  public Condo<M> mask(final Predicate<M> predicate) {
    synchronized (maskLock) {
      masks.add(predicate);
      maskLock.notifyAll();
    }

    return this;
  }

  @Override
  public Condo<M> unmask(final Predicate<M> predicate) {
    synchronized (maskLock) {
      final Iterator<Predicate<M>> iterator = masks.iterator();

      while (iterator.hasNext()) {
        if (iterator.next() == predicate) {
          iterator.remove();
          evaluateDeferredAfterMaskUpdate();
          maskLock.notifyAll();
          return this;
        }
      }
    }

    throw new IllegalStateException("Mask not registered: " + predicate);
  }

  @Override
  public Condo<M> pump(final Predicate<M> predicate) throws InterruptedException {
    synchronized (maskLock) {
      while (true) {
        final Iterator<DeferredAction<M>> it = this.deferred.iterator();

        while (it.hasNext()) {
          final DeferredAction<M> d = it.next();

          if (predicate.test(d.metadata)) {
            it.remove();
            d.runnable.run();
            return this;
          }
        }

        maskLock.wait();
      }
    }
  }

  @Override
  public Condo<M> waitAny(final Predicate<M> predicate) throws InterruptedException {
    synchronized (processedLock) {
      while (!processed.stream().anyMatch(predicate)) {
        processedLock.wait();
      }
    }

    return this;
  }

  @Override
  public Condo<M> waitOnce(final Predicate<M> predicate) throws InterruptedException {
    synchronized (processedLock) {
      while (true) {
        final Iterator<M> iterator = onceProcessed.iterator();

        while (iterator.hasNext()) {
          if (predicate.test(iterator.next())) {
            iterator.remove();
            return this;
          }
        }

        processedLock.wait();
      }
    }
  }

  /**
   * Evaluate the list of deferred action after the list of masks has been updated.
   *
   * Must be invoked under {@link #maskLock}.
   */
  private void evaluateDeferredAfterMaskUpdate() {
    final Iterator<DeferredAction<M>> it = this.deferred.iterator();

    while (it.hasNext()) {
      final DeferredAction<M> d = it.next();

      /* is the current action still masked? */
      if (masks.stream().anyMatch(p -> p.test(d.metadata))) {
        continue;
      }

      it.remove();
      d.runnable.run();
    }
  }

  /**
   * Defer the given action.
   *
   * @param metadata Metadata associated with the action.
   * @param action Action to defer.
   * @param future Future that will be bound to the action.
   * @param <T> Return type of the action.
   * @return A defferred action container.
   */
  private <T> DeferredAction<M> deferAction(
      final M metadata, final Supplier<? extends CompletionStage<T>> action,
      final CompletableFuture<T> future
  ) {
    return new DeferredAction<>(metadata, () -> {
      final CompletionStage<? extends T> resultFuture;

      try {
        resultFuture = action.get();
      } catch (final Exception e) {
        future.completeExceptionally(e);
        markProcessed(metadata);
        return;
      }

      resultFuture.handleAsync((result, e) -> {
        if (e != null) {
          future.completeExceptionally(e);
        } else {
          future.complete(result);
        }

        markProcessed(metadata);
        return null;
      }, executor);
    });
  }

  /**
   * Mark the given metadata as processed.
   *
   * @param metadata Metadata to mark as processed.
   */
  private void markProcessed(final M metadata) {
    synchronized (processedLock) {
      processed.add(metadata);
      onceProcessed.add(metadata);
      processedLock.notifyAll();
    }
  }

  public static <M> Condo<M> buildDefault() {
    return new Builder<M>().build();
  }

  public static <M> Builder<M> builder() {
    return new Builder<>();
  }

  public static class Builder<M> {
    private Optional<Executor> executor = Optional.empty();

    public Builder<M> executor(final Executor executor) {
      this.executor = Optional.of(executor);
      return this;
    }

    public Condo<M> build() {
      final Executor e = this.executor.orElseGet(ForkJoinPool::commonPool);
      return new CoreCondo<>(e);
    }
  }

  @RequiredArgsConstructor
  static class DeferredAction<M> {
    private final M metadata;
    private final Runnable runnable;
  }
}

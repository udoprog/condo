package eu.toolchain.condo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Main interface to Condo, the conditional execution engine.
 *
 * @param <M> Type of metadata.
 */
public interface Condo<M> {
  /**
   * Schedule an action with some metadata.
   *
   * @param metadata Metadata to schedule action for.
   * @param action Action to schedule.
   * @param <T> Type that action returns.
   * @return A future that will be associated to the execution of the given action.
   */
  <T> CompletableFuture<T> schedule(M metadata, Supplier<T> action);

  /**
   * Schedule an action with some metadata.
   *
   * The scheduled action returns a completable future and the action will only be considered done
   * once the returned future is completed for any reason.
   *
   * @param metadata Metadata to schedule action for.
   * @param action Action to schedule.
   * @param <T> Type that action returns.
   * @return A future that will be associated to the execution of the given action.
   */
  <T> CompletableFuture<T> scheduleAsync(
      M metadata, Supplier<? extends CompletionStage<T>> action
  );

  /**
   * Mask all actions matching the given predicate.
   * Masks prevent actions from being executed.
   *
   * Masks are compared using reference equality (<code>a == b</code>).
   *
   * @param predicate Predicate to unmask.
   */
  Condo<M> mask(Predicate<M> predicate);

  /**
   * Unmask actions matching the given predicate.
   *
   * If there are pending actions that have previously been masked by the given predicate they will
   * be scheduled again. This does not guarantee that the action will be executed since there might
   * be other predicates registered still masking it.
   *
   * Masks are compared using reference equality (<code>a == b</code>).
   *
   * @param predicate Predicate to unmask.
   * @throws java.lang.IllegalStateException if the given predicate is not part of the mask set.
   */
  Condo<M> unmask(Predicate<M> predicate);

  /**
   * Allow a single masked action matching the given predicate to be processed.
   *
   * Will wait until one action has been processed.
   *
   * @param predicate Predicate to pump.
   * @throws java.lang.InterruptedException If waiting for the predicate is interrupted.
   */
  Condo<M> pump(Predicate<M> predicate) throws InterruptedException;

  /**
   * Wait until an action matching the given predicate has been processed.
   *
   * @param predicate Predicate to match against.
   * @throws java.lang.InterruptedException If waiting for the predicate is interrupted.
   */
  Condo<M> waitAny(Predicate<M> predicate) throws InterruptedException;

  /**
   * Wait until an action matching the given predicate has been processed.
   *
   * This wait method only allows one action to be matched once.
   *
   * @param predicate Predicate to match against.
   * @throws java.lang.InterruptedException If waiting for the given predicate is interrupted.
   */
  Condo<M> waitOnce(Predicate<M> predicate) throws InterruptedException;
}

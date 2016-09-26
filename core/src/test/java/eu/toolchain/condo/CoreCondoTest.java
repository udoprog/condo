package eu.toolchain.condo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class CoreCondoTest {
  @Test
  public void testBuildDefault() {
    final Condo<Meta> condo = CoreCondo.buildDefault();

    assertThat(condo, instanceOf(CoreCondo.class));
  }

  @Test
  public void testBuilder() {
    final Condo<Meta> condo = CoreCondo.<Meta>builder().build();

    assertThat(condo, instanceOf(CoreCondo.class));
  }

  @Test
  public void testWaitUntil() throws InterruptedException {
    final Condo<Meta> condo = CoreCondo.buildDefault();

    final Meta m = Mockito.mock(Meta.class);

    final CompletableFuture<Void> future = condo.schedule(m, () -> null);

    condo.waitAny(match -> true);
    assertThat(future.isDone(), is(true));
  }

  @Test
  public void testMask() throws InterruptedException {
    final Condo<Meta> condo = CoreCondo.buildDefault();
    final Predicate<Meta> predicate = match -> true;
    condo.mask(predicate);

    final Meta m = Mockito.mock(Meta.class);

    final CompletableFuture<Void> future = condo.schedule(m, () -> null);

    condo.unmask(predicate).waitOnce(predicate);

    assertThat(future.isDone(), is(true));
  }

  @Test(expected = IllegalStateException.class)
  public void testIllegalUnmask() {
    CoreCondo.buildDefault().unmask(match -> true);
  }

  @Test(timeout = 1000L)
  public void testPump() throws InterruptedException {
    final Condo<Meta> condo = CoreCondo.buildDefault();

    final Meta m1 = Mockito.mock(Meta.class);
    final Meta m2 = Mockito.mock(Meta.class);

    final Predicate<Meta> predicate = match -> match == m1;
    condo.mask(predicate);

    final CompletableFuture<Void> f1 = condo.schedule(m1, () -> null);
    final CompletableFuture<Void> f2 = condo.schedule(m2, () -> null);

    ForkJoinPool.commonPool().execute(() -> {
      try {
        Thread.sleep(100);
        condo.pump(predicate);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    condo.waitOnce(match -> match == m2).waitOnce(predicate).unmask(predicate);

    assertThat(f1.isDone(), is(true));
    assertThat(f2.isDone(), is(true));
  }

  interface Meta {
  }
}

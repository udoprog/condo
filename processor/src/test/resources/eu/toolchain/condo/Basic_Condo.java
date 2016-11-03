package eu.toolchain.condo;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Generated;

@Generated("eu.toolchain.condo.CondoProcessor")
class Basic_Condo implements Basic {
  private final Condo<BasicMetadata> condo;
  private final Basic delegate;

  public Basic_Condo(final Condo<BasicMetadata> condo, final Basic delegate) {
    this.condo = condo;
    this.delegate = delegate;
  }

  @Override
  public void doSomething() {
    condo.schedule(new BasicMetadata.DoSomething(), () -> { delegate.doSomething(); return null; });
  }

  @Override
  public boolean checkSomething() {
    return delegate.checkSomething();
  }

  @Override
  public CompletableFuture<Void> getInteger(final int argument) {
    return condo.scheduleAsync(new BasicMetadata.GetInteger(argument), () -> delegate.getInteger(argument));
  }

  @Override
  public CompletableFuture<Void> skipParameter(final int argument, final int ignored) {
    return condo.scheduleAsync(new BasicMetadata.SkipParameter(argument), () -> delegate.skipParameter(argument, ignored));
  }
}

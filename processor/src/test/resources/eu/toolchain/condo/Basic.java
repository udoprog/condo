package eu.toolchain.condo;

import java.util.concurrent.CompletableFuture;

@AutoCondo
interface Basic {
  CompletableFuture<Void> getInteger(int argument);

  CompletableFuture<Void> skipParameter(int argument, @AutoCondo.Skip int ignored);
}

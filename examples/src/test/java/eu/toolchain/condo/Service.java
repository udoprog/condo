package eu.toolchain.condo;

import com.google.common.base.Throwables;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class Service {
  private final ForkJoinPool commonPool = ForkJoinPool.commonPool();

  private final Database database;

  public void put(final String id, final Entity entity) {
    commonPool.execute(() -> {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        throw Throwables.propagate(e);
      }

      database.write(id, entity);
    });
  }

  public Entity get(final String id) {
    return database.read(id);
  }
}

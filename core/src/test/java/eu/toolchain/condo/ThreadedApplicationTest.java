package eu.toolchain.condo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ThreadedApplicationTest {
  final Entity entity = new Entity("1");

  private Condo<String> condo;
  private Database database;
  private MyThreadedApplication app;

  @Rule
  public Timeout globalTimeout = new Timeout(500);

  @Before
  public void setUp() {
    condo = CoreCondo.buildDefault();
    database = Mockito.mock(Database.class);
    app = new MyThreadedApplication(new CondoDatabase(condo, database));

    final CompletableFuture<Void> write = completedFuture(null);
    doReturn(write).when(database).write(any(Entity.class));
  }

  @Test
  public void testOrder() throws Exception {
    final Predicate<String> predicate = m -> m.equals("write:1");

    app.doSomething(entity);

    condo.waitOnce(predicate);

    /* the entity with id 1 is not guaranteed to have been written */
    verify(database).write(any(Entity.class));
  }

  @Test
  public void testDeferAction() throws Exception {
    final Predicate<String> predicate = m -> m.equals("write:1");

    condo.mask(predicate);

    app.doSomething(entity);

    /* allow the write to go through and wait for it to happen */
    condo.unmask(predicate).waitOnce(predicate);

    /* the entity with id 1 is not guaranteed to have been written */
    verify(database).write(any(Entity.class));
  }

  @Test
  public void testMaskedPump() throws Exception {
    final Predicate<String> predicate = m -> m.equals("write:1");

    condo.mask(predicate);

    app.doSomething(entity);

    /* allow the write to go through and wait for it to happen */
    condo.pump(predicate).waitOnce(predicate);

    /* the entity with id 1 is not guaranteed to have been written */
    verify(database).write(any(Entity.class));
  }

  /* declared in your project */
  static class MyThreadedApplication {
    private final Database database;

    public MyThreadedApplication(final Database database) {
      this.database = database;
    }

    private final Executor executor = ForkJoinPool.commonPool();

    public void doSomething(final Entity entity) {
      /* How can we guarantee that the database write has occured? */
      executor.execute(() -> {
        database.write(entity);
      });
    }
  }

  /* declared in your project */
  interface Database {
    CompletableFuture<Void> write(Entity entity);
  }

  static class Entity {
    private final String id;

    public Entity(final String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }
  }

  static class CondoDatabase implements Database {
    private final Condo<String> condo;
    private final Database database;

    public CondoDatabase(final Condo<String> condo, final Database database) {
      this.condo = condo;
      this.database = database;
    }

    @Override
    public CompletableFuture<Void> write(final Entity entity) {
      return condo.scheduleAsync("write:" + entity.getId(), () -> database.write(entity));
    }
  }
}

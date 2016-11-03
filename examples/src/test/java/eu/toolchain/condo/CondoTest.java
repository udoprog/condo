package eu.toolchain.condo;

import com.google.common.base.Throwables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CondoTest {
  final Entity entity = new Entity("John");

  private Database database;
  private Condo<DatabaseMetadata> condo;
  private Service service;

  @Before
  public void setUp() {
    database = Mockito.mock(Database.class);
    condo = CoreCondo.buildDefault();
    service = new Service(new Database_Condo(condo, database));
  }

  @Test
  public void testWriteNever() {
    service.put("hello", entity);
    /* operation takes 500ms, this will _probably_ never pass */
    verify(database, never()).write("hello", entity);
  }

  @Test
  public void testWriteWait() throws Exception {
    service.put("hello", entity);
    condo.waitOnce(m -> m instanceof DatabaseMetadata.Write);
    verify(database, times(1)).write("hello", entity);
  }

  @Test
  public void testWaitForSpecificEntity() throws Exception {
    ForkJoinPool.commonPool().execute(() -> {
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        throw Throwables.propagate(e);
      }

      service.put("hello", entity);
    });

    final Predicate<DatabaseMetadata> writeWorld = writeEntity("world");

    condo.mask(writeWorld);

    service.put("world", entity);
    condo.waitOnce(writeEntity("hello"));

    verify(database, times(1)).write("hello", entity);
    verify(database, never()).write("world", entity);

    condo.pump(writeWorld);

    verify(database, times(1)).write("hello", entity);
    verify(database, times(1)).write("world", entity);
  }

  public static Predicate<DatabaseMetadata> writeEntity(final String id) {
    return m -> {
      if (!(m instanceof DatabaseMetadata.Write)) {
        return false;
      }

      return ((DatabaseMetadata.Write) m).id().equals(id);
    };
  }
}

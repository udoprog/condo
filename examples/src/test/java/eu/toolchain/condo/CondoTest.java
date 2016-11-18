package eu.toolchain.condo;

import com.google.common.base.Throwables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CondoTest {
  final Entity entity = new Entity("John");

  private InMemoryDatabase database;
  private Condo<DatabaseMetadata> condo;
  private Service service;

  @Before
  public void setUp() {
    database = new InMemoryDatabase();
    condo = CoreCondo.buildDefault();
    service = new Service(new Database_Condo(condo, database));
  }

  @Test
  public void testWriteNever() {
    service.put("hello", entity);
    /* operation takes 50ms, this will _probably_ never pass */
    assertNull(database.read("hello"));
  }

  @Test
  public void testWriteWait() throws Exception {
    service.put("hello", entity);
    condo.waitOnce(m -> m instanceof DatabaseMetadata.Write);
    assertEquals(entity, database.read("hello"));
  }

  @Test
  public void testWaitForSpecificEntity() throws Exception {
    ForkJoinPool.commonPool().execute(() -> {
      service.put("hello", entity);
    });

    final Predicate<DatabaseMetadata> writeWorld = writeEntity("world");

    condo.mask(writeWorld);

    service.put("world", entity);
    condo.waitOnce(writeEntity("hello"));

    assertEquals(entity, database.read("hello"));
    assertNull(database.read("world"));

    condo.pump(writeWorld).waitOnce(writeWorld);

    assertEquals(entity, database.read("hello"));
    assertEquals(entity, database.read("world"));
  }

  static Predicate<DatabaseMetadata> writeEntity(final String id) {
    return m -> m instanceof DatabaseMetadata.Write && ((DatabaseMetadata.Write) m).id().equals(id);
  }
}

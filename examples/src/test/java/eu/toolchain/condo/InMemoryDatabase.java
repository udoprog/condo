package eu.toolchain.condo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryDatabase implements Database {
  private final ConcurrentMap<String, Entity> entities = new ConcurrentHashMap<>();

  @Override
  public void write(final String id, final Entity entity) {
    entities.put(id, entity);
  }

  @Override
  public Entity read(final String id) {
    return entities.get(id);
  }
}

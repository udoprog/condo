package eu.toolchain.condo;

@AutoCondo
public interface Database {
  void write(String id, Entity entity);

  Entity read(String id);
}

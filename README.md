# Condo

[![Build Status](https://travis-ci.org/udoprog/condo.svg?branch=master)](https://travis-ci.org/udoprog/condo)

Condo, The Conditional Event Pump for Java.

This is a small library providing a configurable, deterministic event pump.
It allows you to build processes that can be orchestrated inside of tests for
things to happen in a particular order, greatly helping make complex
integration tests deterministic.

## Example

With condo every action is described with _metadata_.
This can be anything that you can match a predicate against, but the simplest
case is a string as showcased in
[ThreadedApplicationTest][threaded-application-test].

[threaded-application-test]: /core/src/test/java/eu/toolchain/condo/ThreadedApplicationTest.java

For even more detailed use-cases, read the JavaDoc in
[the Condo class][condo-api] or checkout [the test cases][condo-tests]

[condo-api]: /api/src/main/java/eu/toolchain/condo/Condo.java
[condo-tests]: /core/src/test/java/eu/toolchain/condo/CoreCondoTest.java

## @AutoCondo annotation

Any interfaces annotated with `@AutoCondo` will cause a `<name>_Condo` class
and `<name>Metadata` interface to be generated.

These are implementations that are intended to wrap the annotated interfaces to
provide a condo-based implementation.

```java
@AutoCondo
interface Database {
  public CompletableFuture<Void> write(final Entity entity);
}
```

In your code, you can now provide the following delegate implementation.

```java
final Entity entity = Mockito.mock(Entity.class);
final Database mockDatabase = Mockito.mock(Database.class);
final Condo<DatabaseMetadata> condo = CoreCondo.buildDefault();

doReturn(CompletableFuture.completedFuture(null)).when(mockDatabase).write(entity);

final Database database = new Database_Condo(condo, mockDatabase);
final Predicate<DatabaseMetadata> anyWrite = m -> m instanceof DatabaseMetadata.Write;

condo.mask(anyWrite);

final CompletableFuture<Void> writeFuture = database.write(entity);

/* writes will never happen */
verify(mockDatabase, never()).write(entity);

condo.pump(anyWrite);

/* will always pass */
verify(mockDatabase).write(entity);
```

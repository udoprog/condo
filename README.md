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

To activate the annotation processor, add a dependency to `condo-processor`.

```xml
<dependencies>
  <dependency>
    <groupId>eu.toolchain.condo</groupId>
    <artifactId>condo-processor</artifactId>
    <version>${condo.version}</version>
  </dependency>
</dependencies>
```

The following example will showcase how this works with a `Database` interface.

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

There are certain guarantees provided by this code.
Now you can control which writes are performed in which order.
This is a fundamental property of writing stable integration tests.

The `Database` interface in isolation is not terribly interesting, but consider
if it is part of a larger project.
Now you can wait until certan preconditions are true, before doing a certain
action.

```java
final Condo<DatabaseMetadata> condo = /*  */;
final Service service  = /*  */;

final Predicate<DatabaseMetadata> anyUpload = m -> m instanceof DatabaseMetadata.Upload;

final String content = /*  */;

service.request('PUT', '/upload', content);

/* wait until an upload has been processed by the database before attempting to
   perform a request */
condo.waitOnce(anyUpload);

service.request('GET', '/search').join()
```

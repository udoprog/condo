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

For examples on how to use it, see [condo-examples][examples]

[examples]: /examples/src/test/java/eu/toolchain/condo

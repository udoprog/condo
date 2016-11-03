package eu.toolchain.condo;

import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.text.MessageFormat;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class CondoProcessorTest {
  @Test
  public void testEmpty() {
    verifySerializer("Empty");
  }

  @Test
  public void testBasic() {
    verifySerializer("Basic");
  }

  static void verifySerializer(String name) {
    verifySerializer(name, MessageFormat.format(CondoProcessor.IMPLEMENTATION_NAME_FORMAT, name),
        MessageFormat.format(CondoProcessor.METADATA_NAME_FORMAT, name));
  }

  static void verifySerializer(String sourceName, String first, String... rest) {
    final JavaFileObject source = resourcePathFor(sourceName);
    final JavaFileObject firstSerializer = resourcePathFor(first);

    final JavaFileObject restSerializers[] = new JavaFileObject[rest.length];

    for (int i = 0; i < rest.length; i++) {
      restSerializers[i] = resourcePathFor(rest[i]);
    }

    assert_()
        .about(javaSource())
        .that(source)
        .processedWith(new CondoProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(firstSerializer, restSerializers);
  }

  static void verifyFailingSerializer(final String name) {
    final JavaFileObject source = resourcePathFor(name);
    assert_().about(javaSource()).that(source).processedWith(new CondoProcessor()).failsToCompile();
  }

  static JavaFileObject resourcePathFor(String name) {
    final String dirName = CondoProcessorTest.class.getPackage().getName().replace('.', '/');
    return JavaFileObjects.forResource(String.format("%s/%s.java", dirName, name));
  }
}

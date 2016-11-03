package eu.toolchain.condo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes annotated with @AutoCondo will cause a generated class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoCondo {
  /**
   * Do not record the given parameter in the metadata object.
   */
  @Target(ElementType.PARAMETER)
  @Retention(RetentionPolicy.SOURCE)
  @interface Skip {
  }
}

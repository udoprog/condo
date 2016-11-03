package eu.toolchain.condo;

import javax.annotation.Generated;

@Generated("eu.toolchain.condo.CondoProcessor")
class Empty_Condo implements Empty {
  private final Condo<EmptyMetadata> condo;
  private final Empty delegate;

  public Empty_Condo(final Condo<EmptyMetadata> condo, final Empty delegate) {
    this.condo = condo;
    this.delegate = delegate;
  }
}

package eu.toolchain.condo;

import javax.annotation.Generated;

@Generated("eu.toolchain.condo.CondoProcessor")
interface BasicMetadata {
  class GetInteger implements BasicMetadata {
    private final int argument;

    public GetInteger(final int argument) {
      this.argument = argument;
    }

    public int argument() {
      return argument;
    }
  }

  class SkipParameter implements BasicMetadata {
    private final int argument;

    public GetInteger(final int argument) {
      this.argument = argument;
    }

    public int argument() {
      return argument;
    }
  }
}

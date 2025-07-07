package io.avaje.simplelogger.encoder;

final class AbbreviatorShortName implements Abbreviator {

  @Override
  public String abbreviate(String fullName) {
    int lastIndex = fullName.lastIndexOf('.');
    return lastIndex != -1 ? fullName.substring(lastIndex + 1) : fullName;
  }
}

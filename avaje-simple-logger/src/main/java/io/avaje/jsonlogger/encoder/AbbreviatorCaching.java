package io.avaje.jsonlogger.encoder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class AbbreviatorCaching implements Abbreviator {

  private final Abbreviator delegate;

  private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

  AbbreviatorCaching(Abbreviator delegate) {
    super();
    this.delegate = delegate;
  }

  @Override
  public String abbreviate(String in) {
    return cache.computeIfAbsent(in, delegate::abbreviate);
  }

}

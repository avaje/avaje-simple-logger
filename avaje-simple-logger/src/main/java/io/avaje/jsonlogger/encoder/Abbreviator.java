package io.avaje.jsonlogger.encoder;

interface Abbreviator {

    Abbreviator NOOP = new NoopAbbreviator();

    String abbreviate(String name);

    static Abbreviator create(int targetLength) {
        if (targetLength < 0 || targetLength == Integer.MAX_VALUE) {
            return NOOP;
        }
        final Abbreviator abbreviator;
        if (targetLength == 0) {
            abbreviator = new AbbreviatorShortName();
        } else {
            abbreviator = new AbbreviatorByLength(targetLength);
        }
        return new AbbreviatorCaching(abbreviator);
    }

    final class NoopAbbreviator implements Abbreviator {
        @Override
        public String abbreviate(String in) {
            return in;
        }
    }
}

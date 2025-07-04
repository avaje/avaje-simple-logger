package io.avaje.simplelogger.encoder;

interface Abbreviator {

    Abbreviator NOOP = new NoopAbbreviator();

    String abbreviate(String name);

    static Abbreviator create(String targetLength) {
        if (targetLength == null || "full".equalsIgnoreCase(targetLength)) {
            return NOOP;
        }
        if ("short".equalsIgnoreCase(targetLength)) {
            return shortName();
        }
        try {
            return create(Integer.parseInt(targetLength));
        } catch (NumberFormatException e) {
            return NOOP;
        }
    }

    static Abbreviator create(int targetLength) {
        if (targetLength < 0 || targetLength == Integer.MAX_VALUE) {
            return NOOP;
        }
        if (targetLength == 0) {
            return shortName();
        } else {
            return new AbbreviatorCaching(new AbbreviatorByLength(targetLength));
        }
    }

    private static AbbreviatorCaching shortName() {
        return new AbbreviatorCaching(new AbbreviatorShortName());
    }

    final class NoopAbbreviator implements Abbreviator {
        @Override
        public String abbreviate(String in) {
            return in;
        }
    }
}

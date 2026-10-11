package net.kalbskinder.eyeliner.style;

/**
 * A size in blocks, a percentage of a reference size, or {@code auto}.
 */
public sealed interface Length {

    Auto AUTO = new Auto();

    static Length blocks(double value) {
        return new Blocks(value);
    }

    static Length percent(double value) {
        return new Percent(value);
    }

    /**
     * Parses {@code auto}, {@code 1.5} (blocks), {@code 1.5b} (blocks) or {@code 50%}.
     *
     * @throws IllegalArgumentException if the value is invalid
     */
    static Length parse(String value) {
        String v = value.strip();
        if (v.equalsIgnoreCase("auto")) return AUTO;
        try {
            if (v.endsWith("%")) return percent(Double.parseDouble(v.substring(0, v.length() - 1)));
            if (v.endsWith("b")) v = v.substring(0, v.length() - 1);
            return blocks(Double.parseDouble(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a length: " + value);
        }
    }

    /**
     * Resolves this length against a reference size, or returns {@code fallback} for {@code auto}.
     */
    double resolve(double reference, double fallback);

    default boolean isAuto() {
        return this instanceof Auto;
    }

    record Blocks(double value) implements Length {
        @Override
        public double resolve(double reference, double fallback) {
            return value;
        }
    }

    record Percent(double value) implements Length {
        @Override
        public double resolve(double reference, double fallback) {
            return reference * value / 100.0;
        }
    }

    final class Auto implements Length {
        private Auto() {
        }

        @Override
        public double resolve(double reference, double fallback) {
            return fallback;
        }

        @Override
        public String toString() {
            return "auto";
        }
    }
}

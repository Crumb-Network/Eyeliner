package net.kalbskinder.eyeliner.style;

/**
 * Padding or margin in blocks.
 */
public record Insets(double top, double right, double bottom, double left) {

    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    public double horizontal() {
        return left + right;
    }

    public double vertical() {
        return top + bottom;
    }
}

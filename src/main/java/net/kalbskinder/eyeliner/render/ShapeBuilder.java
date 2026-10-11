package net.kalbskinder.eyeliner.render;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Splits a (rounded, optionally bordered) rectangle into axis-aligned, non-overlapping rectangles that can each
 * be drawn with one text display background. Rounded corners are approximated with horizontal steps.
 * Because pieces never overlap, semi-transparent fills and outlines render without darker seams.
 */
public final class ShapeBuilder {

    /**
     * Height of one corner step in blocks; smaller is smoother but uses more entities.
     */
    private static final double STEP = 0.03;
    private static final int MAX_STEPS = 6;
    private static final double EPSILON = 1e-5;

    private ShapeBuilder() {
    }

    /**
     * A rectangle in local coordinates (y up).
     *
     * @param fill true for the inner fill, false for border pieces
     */
    public record Piece(double left, double bottom, double width, double height, boolean fill) {
    }

    /**
     * @param left   left edge (local coordinates)
     * @param bottom bottom edge (local coordinates)
     * @param radius corner radius, clamped to half of the smaller side
     * @param border border thickness, 0 for none
     */
    public static List<Piece> build(double left, double bottom, double width, double height, double radius, double border) {
        if (width <= EPSILON || height <= EPSILON) return List.of();
        final RoundedRect outer = new RoundedRect(left, bottom, width, height, radius);
        final RoundedRect inner = border > EPSILON && width > 2 * border && height > 2 * border
                ? new RoundedRect(left + border, bottom + border, width - 2 * border, height - 2 * border, Math.max(0, outer.radius - border))
                : null;
        final boolean hasBorder = border > EPSILON;

        final TreeSet<Double> breakpoints = new TreeSet<>();
        outer.addBreakpoints(breakpoints);
        if (inner != null) inner.addBreakpoints(breakpoints);

        final List<Piece> pieces = new ArrayList<>();
        Double previous = null;
        for (double y : breakpoints) {
            if (previous != null && y - previous > EPSILON) {
                final double mid = (previous + y) / 2;
                final double[] o = outer.span(mid);
                final double[] i = inner != null ? inner.span(mid) : null;
                if (o != null) {
                    if (!hasBorder) {
                        add(pieces, o[0], o[1], previous, y, true);
                    } else if (i == null) {
                        add(pieces, o[0], o[1], previous, y, false);
                    } else {
                        add(pieces, i[0], i[1], previous, y, true);
                        add(pieces, o[0], i[0], previous, y, false);
                        add(pieces, i[1], o[1], previous, y, false);
                    }
                }
            }
            previous = y;
        }
        return pieces;
    }

    /**
     * Adds a slice, merging it with the piece directly below when it has the same horizontal extent.
     */
    private static void add(List<Piece> pieces, double x0, double x1, double y0, double y1, boolean fill) {
        if (x1 - x0 <= EPSILON) return;
        for (int k = pieces.size() - 1; k >= 0; k--) {
            final Piece p = pieces.get(k);
            if (p.fill() == fill && Math.abs(p.left() - x0) < EPSILON && Math.abs(p.left() + p.width() - x1) < EPSILON
                    && Math.abs(p.bottom() + p.height() - y0) < EPSILON) {
                pieces.set(k, new Piece(p.left(), p.bottom(), p.width(), y1 - p.bottom(), fill));
                return;
            }
        }
        pieces.add(new Piece(x0, y0, x1 - x0, y1 - y0, fill));
    }

    private static final class RoundedRect {
        final double left, bottom, width, height, radius;
        final int steps;

        RoundedRect(double left, double bottom, double width, double height, double radius) {
            this.left = left;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
            this.radius = Math.clamp(radius, 0, Math.min(width, height) / 2);
            this.steps = this.radius < EPSILON ? 0 : (int) Math.clamp(Math.ceil(this.radius / STEP), 2, MAX_STEPS);
        }

        void addBreakpoints(TreeSet<Double> out) {
            out.add(bottom);
            out.add(bottom + height);
            for (int i = 1; i <= steps; i++) {
                final double d = radius * i / steps;
                out.add(bottom + radius - d);
                out.add(bottom + height - radius + d);
            }
        }

        /**
         * Horizontal extent at height {@code y}, or null outside of the shape.
         */
        double[] span(double y) {
            if (y < bottom || y > bottom + height) return null;
            double depth = 0; // how far into a corner zone we are, 0 = straight edge
            if (steps > 0) {
                if (y < bottom + radius) depth = bottom + radius - y;
                else if (y > bottom + height - radius) depth = y - (bottom + height - radius);
            }
            double inset = 0;
            if (depth > 0) {
                final double stepHeight = radius / steps;
                final int step = Math.min(steps - 1, (int) (depth / stepHeight));
                final double dy = (step + 0.5) * stepHeight;
                inset = radius - Math.sqrt(Math.max(0, radius * radius - dy * dy));
            }
            return new double[]{left + inset, left + width - inset};
        }
    }
}

package net.kalbskinder.eyeliner.style;

import java.util.Locale;

/**
 * Layout enums used by {@link Style}.
 */
public final class Alignment {

    private Alignment() {
    }

    public enum Direction {
        ROW, COLUMN;

        static Direction parse(String value) {
            return switch (value.strip().toLowerCase(Locale.ROOT)) {
                case "row", "horizontal" -> ROW;
                case "column", "col", "vertical" -> COLUMN;
                default -> throw new IllegalArgumentException("expected row or column");
            };
        }
    }

    public enum Horizontal {
        LEFT, CENTER, RIGHT;

        static Horizontal parse(String value) {
            return switch (value.strip().toLowerCase(Locale.ROOT)) {
                case "left", "start" -> LEFT;
                case "center", "middle" -> CENTER;
                case "right", "end" -> RIGHT;
                default -> throw new IllegalArgumentException("expected left, center or right");
            };
        }
    }

    public enum Vertical {
        TOP, CENTER, BOTTOM;

        static Vertical parse(String value) {
            return switch (value.strip().toLowerCase(Locale.ROOT)) {
                case "top", "start" -> TOP;
                case "center", "middle" -> CENTER;
                case "bottom", "end" -> BOTTOM;
                default -> throw new IllegalArgumentException("expected top, center or bottom");
            };
        }
    }
}

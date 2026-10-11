package net.kalbskinder.eyeliner.layout;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Measures text the way the vanilla client lays it out in a text display, using the advances of
 * the default font (glyph width + 1px spacing, +1px when bold). Characters outside the table are
 * assumed to be 6px wide, which is a good approximation for most of the BMP.
 */
public final class FontMetrics {

    /**
     * Height of one line in font pixels (9px glyph cell + 1px spacing).
     */
    public static final int LINE_HEIGHT = 10;

    /**
     * Size of one font pixel in blocks for a text display with scale 1.
     */
    public static final double PIXEL = 0.025;

    private static final int DEFAULT_ADVANCE = 6;
    private static final int[] ASCII = new int[128];

    static {
        java.util.Arrays.fill(ASCII, DEFAULT_ADVANCE);
        set(2, "!,.:;i|'");
        set(3, "l`");
        set(4, " \"()*I[]t{}");
        set(5, "<>fk");
        set(7, "@~");
    }

    private FontMetrics() {
    }

    private static void set(int advance, String chars) {
        for (char c : chars.toCharArray()) ASCII[c] = advance;
    }

    public static int advance(int codePoint, boolean bold) {
        final int base = codePoint < ASCII.length ? ASCII[codePoint] : DEFAULT_ADVANCE;
        return bold && codePoint != ' ' ? base + 1 : base;
    }

    /**
     * Result of laying out text.
     *
     * @param lines        number of lines after wrapping
     * @param maxLineWidth width of the widest line in font pixels
     */
    public record Measurement(int lines, int maxLineWidth) {
    }

    /**
     * Wraps the component at {@code maxWidth} font pixels (greedy, breaking at spaces like the client)
     * and returns the resulting line count and width.
     */
    public static Measurement measure(Component component, int maxWidth) {
        final List<int[]> glyphs = new ArrayList<>(); // [codePoint, advance]
        flatten(component, false, glyphs);

        int lines = 0;
        int maxLineWidth = 0;
        int start = 0;
        final int n = glyphs.size();
        while (start <= n) {
            int width = 0;
            int lastSpace = -1;
            int widthBeforeSpace = 0;
            int end = start;
            int next = n + 1; // index where the next line starts
            int lineWidth = -1;
            for (; end < n; end++) {
                final int cp = glyphs.get(end)[0];
                if (cp == '\n') {
                    lineWidth = width;
                    next = end + 1;
                    break;
                }
                if (cp == ' ') {
                    lastSpace = end;
                    widthBeforeSpace = width;
                }
                final int advance = glyphs.get(end)[1];
                if (width + advance > maxWidth && end > start) {
                    if (lastSpace > start) {
                        lineWidth = widthBeforeSpace;
                        next = lastSpace + 1;
                    } else {
                        lineWidth = width;
                        next = end;
                    }
                    break;
                }
                width += advance;
            }
            if (lineWidth < 0) {
                lineWidth = width;
                next = n + 1;
            }
            lines++;
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
            if (next > n) break;
            start = next;
        }
        return new Measurement(Math.max(1, lines), maxLineWidth);
    }

    private static void flatten(Component component, boolean parentBold, List<int[]> out) {
        final TextDecoration.State state = component.style().decoration(TextDecoration.BOLD);
        final boolean bold = state == TextDecoration.State.NOT_SET ? parentBold : state == TextDecoration.State.TRUE;
        if (component instanceof TextComponent text) {
            text.content().codePoints().forEach(cp -> out.add(new int[]{cp, cp == '\n' ? 0 : advance(cp, bold)}));
        }
        for (Component child : component.children()) flatten(child, bold, out);
    }
}

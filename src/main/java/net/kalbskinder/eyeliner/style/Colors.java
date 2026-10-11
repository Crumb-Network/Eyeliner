package net.kalbskinder.eyeliner.style;

import java.util.Locale;
import java.util.Map;

/**
 * Color parsing and manipulation. All colors are packed ARGB integers.
 */
public final class Colors {

    public static final int TRANSPARENT = 0;

    private static final Map<String, Integer> NAMED = Map.ofEntries(
            Map.entry("transparent", 0x00000000),
            Map.entry("none", 0x00000000),
            Map.entry("black", 0xFF000000),
            Map.entry("white", 0xFFFFFFFF),
            Map.entry("red", 0xFFFF5555),
            Map.entry("dark_red", 0xFFAA0000),
            Map.entry("green", 0xFF55FF55),
            Map.entry("dark_green", 0xFF00AA00),
            Map.entry("blue", 0xFF5555FF),
            Map.entry("dark_blue", 0xFF0000AA),
            Map.entry("yellow", 0xFFFFFF55),
            Map.entry("gold", 0xFFFFAA00),
            Map.entry("aqua", 0xFF55FFFF),
            Map.entry("dark_aqua", 0xFF00AAAA),
            Map.entry("light_purple", 0xFFFF55FF),
            Map.entry("dark_purple", 0xFFAA00AA),
            Map.entry("gray", 0xFFAAAAAA),
            Map.entry("grey", 0xFFAAAAAA),
            Map.entry("dark_gray", 0xFF555555),
            Map.entry("dark_grey", 0xFF555555)
    );

    private Colors() {
    }

    /**
     * Parses {@code #RGB}, {@code #RGBA}, {@code #RRGGBB}, {@code #RRGGBBAA} (CSS order, alpha last)
     * or a Minecraft color name such as {@code gold}.
     *
     * @throws IllegalArgumentException if the value is not a valid color
     */
    public static int parse(String value) {
        final String v = value.strip().toLowerCase(Locale.ROOT);
        final Integer named = NAMED.get(v);
        if (named != null) return named;
        if (!v.startsWith("#")) throw new IllegalArgumentException("not a color: " + value);

        String hex = v.substring(1);
        if (hex.length() == 3 || hex.length() == 4) {
            StringBuilder expanded = new StringBuilder();
            for (char c : hex.toCharArray()) expanded.append(c).append(c);
            hex = expanded.toString();
        }
        if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("not a color: " + value);
        final long parsed;
        try {
            parsed = Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a color: " + value);
        }
        if (hex.length() == 6) return (int) (0xFF000000L | parsed);
        // RRGGBBAA -> AARRGGBB
        return (int) (((parsed & 0xFF) << 24) | (parsed >>> 8));
    }

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    public static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }

    public static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0xFFFFFF);
    }

    /**
     * Mixes the color towards white by {@code amount} (0..1), keeping alpha.
     */
    public static int lighten(int argb, double amount) {
        return mix(argb, 0xFFFFFF, amount);
    }

    /**
     * Mixes the color towards black by {@code amount} (0..1), keeping alpha.
     */
    public static int darken(int argb, double amount) {
        return mix(argb, 0x000000, amount);
    }

    private static int mix(int argb, int targetRgb, double amount) {
        final int r = channel(argb >> 16, targetRgb >> 16, amount);
        final int g = channel(argb >> 8, targetRgb >> 8, amount);
        final int b = channel(argb, targetRgb, amount);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, double amount) {
        from &= 0xFF;
        to &= 0xFF;
        return (int) Math.round(from + (to - from) * amount);
    }

    /**
     * Relative luminance in 0..1, used to decide whether hovering should lighten or darken.
     */
    public static double luminance(int argb) {
        return (0.2126 * ((argb >> 16) & 0xFF) + 0.7152 * ((argb >> 8) & 0xFF) + 0.0722 * (argb & 0xFF)) / 255.0;
    }
}

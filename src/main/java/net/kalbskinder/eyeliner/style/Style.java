package net.kalbskinder.eyeliner.style;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The fully resolved, immutable style of one element. Every property has a value: anything that is missing
 * or invalid falls back to the tag default (and invalid values produce a warning).
 */
public final class Style {

    private final String tag;
    private final Map<String, String> values;
    private final Map<String, String> defaults;
    private final String location;
    private final Consumer<String> warnings;

    // Box model
    private final Length width;
    private final Length height;
    private final Insets padding;
    private final Insets margin;

    // Child layout
    private final Alignment.Direction direction;
    private final Alignment.Horizontal align;
    private final Alignment.Vertical valign;
    private final double gap;

    // Background
    private final int bg;
    private final Integer hoverBg;
    private final double border;
    private final int borderColor;
    private final Length radius;

    // Text
    private final int color;
    private final double fontSize;
    private final boolean shadow;
    private final Alignment.Horizontal textAlign;

    Style(String tag, Map<String, String> values, Map<String, String> defaults, String location, Consumer<String> warnings) {
        this.tag = tag;
        this.values = Map.copyOf(values);
        this.defaults = Map.copyOf(defaults);
        this.location = location;
        this.warnings = warnings;

        this.width = parse("width", Length::parse);
        this.height = parse("height", Length::parse);
        this.padding = new Insets(nonNegative("pt"), nonNegative("pr"), nonNegative("pb"), nonNegative("pl"));
        this.margin = new Insets(number("mt"), number("mr"), number("mb"), number("ml"));

        this.direction = parse("direction", Alignment.Direction::parse);
        this.textAlign = parse("textalign", Alignment.Horizontal::parse);
        this.align = raw("align").equalsIgnoreCase("auto") ? textAlign : parse("align", Alignment.Horizontal::parse);
        this.valign = parse("valign", Alignment.Vertical::parse);
        this.gap = nonNegative("gap");

        this.bg = color("bg");
        this.hoverBg = raw("hoverbg").equalsIgnoreCase("auto") ? null : color("hoverbg");
        this.border = nonNegative("border");
        this.borderColor = color("bordercolor");
        this.radius = parse("radius", Length::parse);

        this.color = color("color");
        this.fontSize = positive("size");
        this.shadow = bool("shadow");
    }

    // ---- generic, fallback-aware accessors (also used for element specific attributes) ----

    /**
     * The raw merged value, or the default if the key is unknown. Never null for registered keys.
     */
    public String raw(String key) {
        final String value = values.get(key);
        if (value != null) return value;
        final String fallback = defaults.get(key);
        return fallback != null ? fallback : "";
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    /**
     * Parses the value of {@code key}. If it is invalid, a warning is emitted and the default is parsed instead.
     */
    public <T> T parse(String key, Function<String, T> parser) {
        final String value = raw(key);
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            final String fallback = defaults.get(key);
            warnings.accept("Invalid value '" + value + "' for '" + key + "' on " + location
                    + (e.getMessage() != null ? " (" + e.getMessage() + ")" : "")
                    + ", using default '" + fallback + "'");
            if (fallback == null) throw new IllegalStateException("No default for '" + key + "' on <" + tag + ">", e);
            return parser.apply(fallback);
        }
    }

    public double number(String key) {
        return parse(key, v -> {
            final double d = Double.parseDouble(v.strip());
            if (!Double.isFinite(d)) throw new IllegalArgumentException("not a finite number");
            return d;
        });
    }

    public double nonNegative(String key) {
        return parse(key, v -> {
            final double d = Double.parseDouble(v.strip());
            if (!(d >= 0) || !Double.isFinite(d)) throw new IllegalArgumentException("must be >= 0");
            return d;
        });
    }

    public double positive(String key) {
        return parse(key, v -> {
            final double d = Double.parseDouble(v.strip());
            if (!(d > 0) || !Double.isFinite(d)) throw new IllegalArgumentException("must be > 0");
            return d;
        });
    }

    public int color(String key) {
        return parse(key, Colors::parse);
    }

    public boolean bool(String key) {
        return parse(key, v -> switch (v.strip().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new IllegalArgumentException("expected true or false");
        });
    }

    // ---- typed properties ----

    public String tag() {
        return tag;
    }

    public String location() {
        return location;
    }

    public Length width() {
        return width;
    }

    public Length height() {
        return height;
    }

    public Insets padding() {
        return padding;
    }

    public Insets margin() {
        return margin;
    }

    public Alignment.Direction direction() {
        return direction;
    }

    public Alignment.Horizontal align() {
        return align;
    }

    public Alignment.Vertical valign() {
        return valign;
    }

    public double gap() {
        return gap;
    }

    public int bg() {
        return bg;
    }

    /**
     * Background while hovered. Defaults to a lighter (or, for bright colors, darker) version of {@link #bg()}.
     */
    public int hoverBg() {
        if (hoverBg != null) return hoverBg;
        if (Colors.alpha(bg) == 0) return bg;
        return Colors.luminance(bg) > 0.7 ? Colors.darken(bg, 0.15) : Colors.lighten(bg, 0.2);
    }

    public double border() {
        return border;
    }

    public int borderColor() {
        return borderColor;
    }

    public Length radius() {
        return radius;
    }

    public int color() {
        return color;
    }

    /**
     * Font size. 1 unit = 1/20 block of line height, so {@code size="5"} matches vanilla text displays.
     */
    public double fontSize() {
        return fontSize;
    }

    public boolean shadow() {
        return shadow;
    }

    public Alignment.Horizontal textAlign() {
        return textAlign;
    }
}

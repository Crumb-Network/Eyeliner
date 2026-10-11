package net.kalbskinder.eyeliner.style;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Holds default values per tag and named variants, and resolves the final {@link Style} of an element.
 * <p>
 * Resolution order (later wins):
 * <ol>
 *     <li>global defaults (every property has one, so nothing is ever unset)</li>
 *     <li>inherited values from the parent ({@link #INHERITED})</li>
 *     <li>tag defaults (e.g. buttons have a background)</li>
 *     <li>variants, in the order listed in {@code variant="..."}</li>
 *     <li>attributes written on the element</li>
 * </ol>
 * Variants are plain attribute maps, so anything that can be written as an attribute can be part of a variant.
 * Register your own with {@link #registerVariant(String, String, Map)}.
 */
public final class StyleSheet {

    /**
     * Wildcard tag: variants registered for it apply to every element.
     */
    public static final String ANY = "*";

    /**
     * Properties passed down to children unless they override them, like CSS.
     */
    public static final Set<String> INHERITED = Set.of("color", "size", "shadow", "textalign");

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("background", "bg"),
            Map.entry("bgcolor", "bg"),
            Map.entry("background-color", "bg"),
            Map.entry("hover-bg", "hoverbg"),
            Map.entry("hoverbackground", "hoverbg"),
            Map.entry("font-size", "size"),
            Map.entry("fontsize", "size"),
            Map.entry("text-align", "textalign"),
            Map.entry("border-radius", "radius"),
            Map.entry("border-color", "bordercolor"),
            Map.entry("border-width", "border"),
            Map.entry("flex-direction", "direction"),
            Map.entry("p", "padding"),
            Map.entry("m", "margin"),
            Map.entry("padding-top", "pt"),
            Map.entry("padding-right", "pr"),
            Map.entry("padding-bottom", "pb"),
            Map.entry("padding-left", "pl"),
            Map.entry("margin-top", "mt"),
            Map.entry("margin-right", "mr"),
            Map.entry("margin-bottom", "mb"),
            Map.entry("margin-left", "ml"),
            Map.entry("on-click", "onclick"),
            Map.entry("action", "onclick")
    );

    private static final Map<String, String> GLOBAL_DEFAULTS = Map.ofEntries(
            Map.entry("width", "auto"),
            Map.entry("height", "auto"),
            Map.entry("pt", "0"), Map.entry("pr", "0"), Map.entry("pb", "0"), Map.entry("pl", "0"),
            Map.entry("mt", "0"), Map.entry("mr", "0"), Map.entry("mb", "0"), Map.entry("ml", "0"),
            Map.entry("direction", "column"),
            Map.entry("align", "auto"),
            Map.entry("valign", "center"),
            Map.entry("gap", "0"),
            Map.entry("bg", "transparent"),
            Map.entry("hoverbg", "auto"),
            Map.entry("border", "0"),
            Map.entry("bordercolor", "#ffffff"),
            Map.entry("radius", "0"),
            Map.entry("color", "#ffffff"),
            Map.entry("size", "2"),
            Map.entry("shadow", "false"),
            Map.entry("textalign", "center")
    );

    private static final Set<String> COMMON_KEYS = Set.of("id", "variant");

    private final Map<String, Map<String, String>> tagDefaults = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> tagKeys = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, String>>> variants = new ConcurrentHashMap<>();

    public StyleSheet() {
        registerTag("screen", Map.ofEntries(
                Map.entry("width", "3"),
                Map.entry("height", "2"),
                Map.entry("padding", "0.15"),
                Map.entry("gap", "0.1"),
                Map.entry("valign", "top"),
                Map.entry("bg", "#14141ee6"),
                Map.entry("distance", "2.5"),
                Map.entry("closedistance", "0"),
                Map.entry("reach", "6"),
                Map.entry("brightness", "15")
        ), Set.of("distance", "closedistance", "reach", "brightness"));
        registerTag("button", Map.ofEntries(
                Map.entry("direction", "row"),
                Map.entry("padding", "0.06 0.12"),
                Map.entry("gap", "0.05"),
                Map.entry("bg", "#3a3a4c"),
                Map.entry("sound", "minecraft:ui.button.click")
        ), Set.of("onclick", "onleftclick", "onrightclick", "sound"));
        registerTag("text", Map.of(), Set.of());
        registerTag("sprite", Map.ofEntries(
                Map.entry("item", "minecraft:barrier"),
                Map.entry("spritesize", "0.25"),
                Map.entry("glint", "false")
        ), Set.of("item", "spritesize", "glint"));

        registerVariant(ANY, "flat", Map.of("radius", "0", "border", "0"));
        registerVariant(ANY, "rounded", Map.of("radius", "35%"));
        registerVariant(ANY, "pill", Map.of("radius", "100%"));
        registerVariant(ANY, "outline", Map.of("bg", "transparent", "border", "0.015", "hoverbg", "#ffffff30"));
        registerVariant(ANY, "ghost", Map.of("bg", "transparent", "hoverbg", "#ffffff30"));
        registerVariant("button", "primary", Map.of("bg", "#2f6fdf"));
        registerVariant("button", "secondary", Map.of("bg", "#4b5563"));
        registerVariant("button", "success", Map.of("bg", "#1f9d4c"));
        registerVariant("button", "danger", Map.of("bg", "#d13b3b"));
    }

    /**
     * Registers (or replaces) the defaults of a tag. Extra keys are element specific attributes
     * that should not produce "unknown attribute" warnings.
     */
    public void registerTag(String tag, Map<String, String> defaults, Set<String> extraKeys) {
        final String key = tag.toLowerCase(Locale.ROOT);
        tagDefaults.put(key, normalize(key, defaults, _ -> {
        }));
        tagKeys.put(key, Set.copyOf(extraKeys));
    }

    /**
     * Registers a named variant for a tag, or for every tag when {@code tag} is {@link #ANY}.
     * The values use the same syntax as attributes in a .eyeliner file.
     */
    public void registerVariant(String tag, String name, Map<String, String> values) {
        final String key = tag.toLowerCase(Locale.ROOT);
        variants.computeIfAbsent(key, _ -> new ConcurrentHashMap<>())
                .put(name.toLowerCase(Locale.ROOT), normalize(key, values, _ -> {
                }));
    }

    public boolean isKnownTag(String tag) {
        return tagDefaults.containsKey(tag);
    }

    /**
     * Resolves the style of an element.
     *
     * @param tag        element tag
     * @param attributes attributes as written in the file
     * @param parent     the parent's style, or null for the root
     * @param location   human readable location used in warnings
     * @param warnings   receives non-fatal problems (invalid values fall back to defaults)
     */
    public Style resolve(String tag, Map<String, String> attributes, Style parent, String location, Consumer<String> warnings) {
        final Map<String, String> defaults = new LinkedHashMap<>(GLOBAL_DEFAULTS);
        defaults.putAll(tagDefaults.getOrDefault(tag, Map.of()));

        final Map<String, String> merged = new LinkedHashMap<>(GLOBAL_DEFAULTS);
        if (parent != null) {
            for (String key : INHERITED) merged.put(key, parent.raw(key));
        }
        merged.putAll(tagDefaults.getOrDefault(tag, Map.of()));

        final String variantAttr = attributes.get("variant");
        if (variantAttr != null) {
            for (String name : variantAttr.strip().toLowerCase(Locale.ROOT).split("\\s+")) {
                if (name.isEmpty()) continue;
                Map<String, String> values = variants.getOrDefault(tag, Map.of()).get(name);
                if (values == null) values = variants.getOrDefault(ANY, Map.of()).get(name);
                if (values == null) {
                    warnings.accept("Unknown variant '" + name + "' on " + location);
                    continue;
                }
                merged.putAll(values);
            }
        }

        final Map<String, String> own = normalize(tag, attributes, warnings);
        final Set<String> extraKeys = tagKeys.getOrDefault(tag, Set.of());
        for (String key : own.keySet()) {
            if (!GLOBAL_DEFAULTS.containsKey(key) && !extraKeys.contains(key) && !COMMON_KEYS.contains(key)) {
                warnings.accept("Unknown attribute '" + key + "' on " + location);
            }
        }
        merged.putAll(own);

        return new Style(tag, merged, defaults, location, warnings);
    }

    /**
     * Lower-cases keys, resolves aliases and expands shorthands (padding, margin, px, my, ...) into
     * longhands so that layers merge correctly: a variant's {@code pl} is overridden by a later {@code padding}.
     */
    private static Map<String, String> normalize(String tag, Map<String, String> attributes, Consumer<String> warnings) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            key = ALIASES.getOrDefault(key, key);
            final String value = entry.getValue();
            if (tag.equals("sprite") && key.equals("size")) key = "spritesize";
            switch (key) {
                case "padding" -> expandBox(out, "p", value, warnings);
                case "margin" -> expandBox(out, "m", value, warnings);
                case "px" -> putAll(out, value, "pl", "pr");
                case "py" -> putAll(out, value, "pt", "pb");
                case "mx" -> putAll(out, value, "ml", "mr");
                case "my" -> putAll(out, value, "mt", "mb");
                default -> out.put(key, value);
            }
        }
        return out;
    }

    private static void expandBox(Map<String, String> out, String prefix, String value, Consumer<String> warnings) {
        final List<String> parts = Arrays.stream(value.strip().split("\\s+")).filter(s -> !s.isEmpty()).toList();
        final String top, right, bottom, left;
        switch (parts.size()) {
            case 1 -> top = right = bottom = left = parts.getFirst();
            case 2 -> {
                top = bottom = parts.get(0);
                right = left = parts.get(1);
            }
            case 3 -> {
                top = parts.get(0);
                right = left = parts.get(1);
                bottom = parts.get(2);
            }
            case 4 -> {
                top = parts.get(0);
                right = parts.get(1);
                bottom = parts.get(2);
                left = parts.get(3);
            }
            default -> {
                warnings.accept("Invalid " + (prefix.equals("p") ? "padding" : "margin") + " '" + value + "', expected 1 to 4 values");
                return;
            }
        }
        out.put(prefix + "t", top);
        out.put(prefix + "r", right);
        out.put(prefix + "b", bottom);
        out.put(prefix + "l", left);
    }

    private static void putAll(Map<String, String> out, String value, String... keys) {
        for (String key : keys) out.put(key, value);
    }
}
